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

            if ("/start".equals(messageText)) {
                handleStartCommand(chatId);
            } else if ("/news".equals(messageText)) {
                // Other commands handling
            }
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
        ZonedDateTime nextRun = nowInKiev.withHour(17).withMinute(0).withSecond(0);
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
    /*
    private List<String> extractNewsBlocks(String htmlContent) {
        List<String> newsBlocks = new ArrayList<>();
        Document document = Jsoup.parse(htmlContent);
        Set<String> titledArticleParagraphs = new HashSet<>();

        // First, process titled news articles

        Elements titledNewsArticles = document.select("div._article.ds-layout-grid");
        for (Element article : titledNewsArticles) {
            StringBuilder newsText = new StringBuilder();
            String headline = article.select("h3._headline").text().trim();
            if (!headline.isEmpty()) {
                newsText.append(headline).append("\n\n");
            }

            Elements paragraphs = article.select("p");
            for (Element paragraph : paragraphs) {
                String paragraphText = paragraph.text().trim();
                newsText.append(paragraphText).append("\n\n");
                titledArticleParagraphs.add(paragraphText);  // Keep track of paragraphs in titled articles
            }

            if (!newsText.toString().trim().isEmpty()) {
                newsBlocks.add(newsText.toString().trim());
            }
        }

        // Then, process standalone <p> tags excluding those in titled articles and the specific unwanted paragraph
        Elements standaloneParagraphs = document.select("p:not(div._article.ds-layout-grid > p)");
        for (Element paragraph : standaloneParagraphs) {
            String paragraphText = paragraph.text().trim();
            if (!paragraphText.isEmpty() && !titledArticleParagraphs.contains(paragraphText) &&
                    !paragraphText.equals("Catch up quickly on the global stories that matter")) {
                newsBlocks.add(paragraphText);
            }
        }

        return newsBlocks;
    }
    */

    private List<String> extractNewsBlocks(String htmlContent) {
        List<String> newsBlocks = new ArrayList<>();
        Document document = Jsoup.parse(htmlContent);

        // Select paragraphs that are direct children of div with class '_gobbet'
        Elements gobbetNewsParagraphs = document.select("div._gobbet > div > p");

        for (Element paragraph : gobbetNewsParagraphs) {
            String paragraphText = paragraph.text().trim();
            if (!paragraphText.isEmpty()) {
                newsBlocks.add(paragraphText);
            }
        }

        return newsBlocks;
    }

    private void sendNewsUpdate(long chatId) {
        try {
            LOGGER.info("Fetching news content for Chat ID: " + chatId);
            String htmlContent = DownloadSmallNews.downloadContent();

            if (htmlContent != null && !htmlContent.isEmpty()) {
                // Process and send the news content to the user
                sendNewsItems(chatId, htmlContent);
            } else {
                sendMessage(chatId, "Currently, there are no news updates available.");
            }
        } catch (IOException e) {
            LOGGER.severe("Error fetching news content: " + e.getMessage());
            sendMessage(chatId, "An error occurred while fetching news updates.");
        }
    }

    /*
    private void sendDailyNews() {
        // Fetch and send daily news to all subscribed users at 9 AM
        try {
            String newsContent = WebContentDownloader.downloadContent();
            for (Long chatId : subscribedUsers) {
                sendLongMessage(chatId, newsContent);
            }
        } catch (IOException e) {
            LOGGER.severe("Error sending daily news: " + e.getMessage());
            e.printStackTrace();
        }
    }

     */

    // Schedule to send daily news
    private void sendDailyNews() {
        // Fetch latest news content
        String newsContent;
        try {
            newsContent = DownloadSmallNews.downloadContent();
        } catch (IOException e) {
            LOGGER.severe("Error in downloading content: " + e.getMessage());
            return;
        }

        // Send news to all subscribed users
        for (Long chatId : subscribedUsers) {
            sendNewsItems(chatId, newsContent);
        }
    }


    // New method to extract and send individual news items
    private void sendNewsItems(long chatId, String htmlContent) {
        Document document = Jsoup.parse(htmlContent);
        Elements newsItems = document.select("div._gobbet > div > p");

        for (Element item : newsItems) {
            String newsText = item.html()
                    .replaceAll("<p>", "")
                    .replaceAll("</p>", "\n")
                    .replaceAll("<strong>", "<b>")
                    .replaceAll("</strong>", "</b>");

            sendHtmlMessage(chatId, newsText);
        }
    }


    private void sendHtmlMessage(Long chatId, String htmlText) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(htmlText);
        message.enableHtml(true); // Enable HTML formatting for the message

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
