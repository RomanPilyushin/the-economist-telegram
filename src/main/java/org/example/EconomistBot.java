package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;


public class EconomistBot extends TelegramLongPollingBot {
    private static final int MAX_MESSAGE_LENGTH = 4096; // Maximum length of a Telegram message
    private static final Logger LOGGER = Logger.getLogger(EconomistBot.class.getName());
    private static final Set<Long> subscribedUsers = new HashSet<>();
    private static String BOT_TOKEN;
    private static String BOT_USERNAME;

    // Jetty server port
    private static final int JETTY_SERVER_PORT = 8080; // Choose an appropriate port

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try {
            // Load properties
            Properties properties = new Properties();
            InputStream inputStream = EconomistBot.class.getClassLoader().getResourceAsStream("config.properties");
            if (inputStream != null) {
                properties.load(inputStream);
                BOT_TOKEN = properties.getProperty("bot.token");
                BOT_USERNAME = properties.getProperty("bot.username");
            } else {
                LOGGER.severe("config.properties file not found");
                throw new IllegalStateException("config.properties file not found");
            }
        } catch (Exception e) {
            LOGGER.severe("Error loading config.properties: " + e.getMessage());
            throw new IllegalStateException("Error loading config.properties", e);
        }
    }

    public EconomistBot() {
        subscribedUsers.addAll(DatabaseUtil.getAllUsers());
        scheduleDailyNews();
    }

    public static void main(String[] args) {
        try {
            // Start the Telegram bot
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new EconomistBot());

            // Start the Jetty server for health checks
            startJettyServer();
        } catch (Exception e) {
            LOGGER.severe("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void startJettyServer() throws Exception {
        // Use the PORT environment variable, default to 8080 if not set
        String portEnv = System.getenv("PORT");
        int port = portEnv != null ? Integer.parseInt(portEnv) : 8080;

        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add servlet to handle health checks
        context.addServlet(new ServletHolder(new HealthCheckServlet()), "/health");

        server.start();
        LOGGER.info("Jetty server started on port " + port);
    }


    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    // Updated onUpdateReceived method to handle news extraction and sending
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            if ("/start".equals(messageText)) { handleStartCommand(chatId); }
            // Add more command handling as needed
        }
    }


    private void handleStartCommand(long chatId) {
        if (!subscribedUsers.contains(chatId)) {
            subscribedUsers.add(chatId);
            DatabaseUtil.addUser(chatId); // Add new user to the database
            sendMessage(chatId, "Welcome! You have been subscribed.");
        }
        sendNewsUpdate(chatId); // Send news update to the user
    }


    /**
     * Sends a long message by splitting it into smaller parts if necessary.
     * @param chatId The chat ID to send the message to.
     * @param longMessage The long message to be sent.
     */
    private void sendLongMessage(Long chatId, String longMessage) {
        int length = longMessage.length();
        for (int start = 0; start < length; start += MAX_MESSAGE_LENGTH) {
            int end = Math.min(start + MAX_MESSAGE_LENGTH, length);
            sendMessage(chatId, longMessage.substring(start, end));
        }
    }

    private void scheduleDailyNews() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        // Adjust to Kiev Time Zone (EET, UTC+2/UTC+3)
        ZoneId kievZoneId = ZoneId.of("Europe/Kiev");
        ZonedDateTime nowInKiev = ZonedDateTime.now(kievZoneId);
        ZonedDateTime nextRun = nowInKiev.withHour(9).withMinute(0).withSecond(0);
        if (nowInKiev.compareTo(nextRun) > 0)
            nextRun = nextRun.plusDays(1);

        Duration initialDelay = Duration.between(nowInKiev, nextRun);
        executorService.scheduleAtFixedRate(this::sendDailyNews, initialDelay.toMinutes(), TimeUnit.DAYS.toMinutes(1), TimeUnit.MINUTES);
    }

    /**
     * Sends a message to a specified chat ID.
     * @param chatId The chat ID to send the message to.
     * @param text The text of the message to send.
     */
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        // No parse mode should be set
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendNewsUpdate(long chatId) {
        try {
            // Fetch small news content
            String smallNewsContent = DownloadSmallNews.downloadContent();
            if (smallNewsContent != null && !smallNewsContent.isEmpty()) {
                LOGGER.info("Fetched small news content");
                List<String> smallNewsItems = extractSmallNewsItems(smallNewsContent);
                for (String newsItem : smallNewsItems) {
                    sendHtmlMessage(chatId, newsItem);
                }
            } else {
                LOGGER.warning("No small news updates available");
                sendMessage(chatId, "Currently, there are no small news updates available.");
            }

            // Fetch big news content
            String bigNewsContent = DownloadBigNews.downloadContent();
            if (bigNewsContent != null && !bigNewsContent.isEmpty()) {
                LOGGER.info("Fetched big news content");
                List<String> newsBlocks = extractNewsBlocks(bigNewsContent);
                for (String newsBlock : newsBlocks) {
                    LOGGER.warning("No big news updates available");
                    sendHtmlMessage(chatId, newsBlock);
                }
            } else {
                sendMessage(chatId, "Currently, there are no detailed news updates available.");
            }
        } catch (IOException e) {
            LOGGER.severe("Error fetching news content: " + e.getMessage());
            sendMessage(chatId, "An error occurred while fetching news updates.");
        }
    }


    private List<String> extractNewsBlocks(String htmlContent) {
        List<String> formattedNewsBlocks = new ArrayList<>();
        Document document = Jsoup.parse(htmlContent);
        Elements articleElements = document.select("div.news-block");

        for (Element article : articleElements) {
            StringBuilder newsBlockBuilder = new StringBuilder();

            // Extract and format the title in bold
            String title = article.select("h2").text();
            if (!title.isEmpty()) {
                newsBlockBuilder.append("<b>").append(title).append("</b>\n");
            }

            // Extract and add image URL
            String imageUrl = article.select("img").attr("src");
            if (!imageUrl.isEmpty()) {
                newsBlockBuilder.append(imageUrl).append("\n");
            }

            // Extract and format text paragraphs
            Elements paragraphs = article.select("p");
            for (Element paragraph : paragraphs) {
                String text = paragraph.text();
                newsBlockBuilder.append(text).append("\n\n");
            }

            formattedNewsBlocks.add(newsBlockBuilder.toString().trim());
        }
        return formattedNewsBlocks;
    }


    private List<String> extractSmallNewsItems(String htmlContent) {
        List<String> newsItems = new ArrayList<>();
        Document document = Jsoup.parse(htmlContent);
        Elements smallNewsElements = document.select("div._gobbet > div > p");

        for (Element item : smallNewsElements) {
            String newsText = item.html()
                    .replaceAll("<p>", "")
                    .replaceAll("</p>", "\n")
                    .replaceAll("<strong>", "<b>")
                    .replaceAll("</strong>", "</b>");
            newsItems.add(newsText);
        }
        return newsItems;
    }

    // Schedule to send daily news
    // Schedule to send daily news
    private void sendDailyNews() {
        try {
            // Fetch the latest small news content
            String smallNewsContent = DownloadSmallNews.downloadContent();
            List<String> smallNewsItems = !smallNewsContent.isEmpty() ? extractSmallNewsItems(smallNewsContent) : Collections.emptyList();

            // Fetch the latest big news content
            String bigNewsContent = DownloadBigNews.downloadContent();
            List<String> newsBlocks = !bigNewsContent.isEmpty() ? extractNewsBlocks(bigNewsContent) : Collections.emptyList();

            // Send the news to all subscribed users
            for (Long chatId : subscribedUsers) {
                smallNewsItems.forEach(item -> sendHtmlMessage(chatId, item));
                newsBlocks.forEach(block -> sendHtmlMessage(chatId, block));
            }
        } catch (IOException e) {
            LOGGER.severe("Error in fetching and sending daily news: " + e.getMessage());
        }
    }

    private void sendHtmlMessage(Long chatId, String htmlText) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(htmlText);
        message.enableHtml(true); // Enable HTML formatting for the message

        try {
            LOGGER.info("Sending HTML message to chat ID: " + chatId + "; Content: " + htmlText.substring(0, Math.min(htmlText.length(), 100)) + "...");
            execute(message);
        } catch (TelegramApiException e) {
            LOGGER.severe("Error sending HTML message: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
