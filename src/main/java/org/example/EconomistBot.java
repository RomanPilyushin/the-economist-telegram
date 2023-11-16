package org.example;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
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
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class EconomistBot extends TelegramLongPollingBot {
    private Set<String> lastFetchedNews = new HashSet<>();
    private static final int MAX_MESSAGE_LENGTH = 4096; // Maximum length of a Telegram message
    private static final Logger LOGGER = Logger.getLogger(EconomistBot.class.getName());
    private static final Set<Long> subscribedUsers = new HashSet<>();
    private static final String BOT_TOKEN = "6530358402:AAG28dxK3SQCjlPIYayi-aysMNAI8oOZcyw";
    private static final String BOT_USERNAME = "https://t.me/Theworldinbrief_bot";

    public EconomistBot() {
        // Schedule the periodic news check every hour
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
        executorService.scheduleAtFixedRate(this::checkForNewsUpdates, 0, 1, TimeUnit.HOURS);

        // Calculate initial delay for the 9 AM schedule
        long initialDelay = Duration.between(LocalTime.now(), LocalTime.of(9, 0)).toMinutes();
        if (initialDelay < 0) {
            initialDelay += Duration.ofDays(1).toMinutes(); // Schedule for next day if time has passed
        }
        executorService.scheduleAtFixedRate(this::sendDailyNews, initialDelay, TimeUnit.DAYS.toMinutes(1), TimeUnit.MINUTES);
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new EconomistBot());
        } catch (TelegramApiException e) {
            LOGGER.severe("Error registering bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String inputText = update.getMessage().getText();

            LOGGER.info("Received message: " + inputText + " from Chat ID: " + chatId);

            if ("/start".equals(inputText)) {
                subscribedUsers.add(chatId);
                LOGGER.info("User subscribed. Chat ID: " + chatId);
                sendNewsUpdate(chatId);
            }
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            if (messageText.equals("/news")) { // Example command
                try {
                    String newsContent = WebContentDownloader.downloadContent();
                    if (newsContent != null && !newsContent.isEmpty()) {
                        sendLongMessage(update.getMessage().getChatId(), newsContent);
                    } else {
                        sendMessage(update.getMessage().getChatId(), "No news content available.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    sendMessage(update.getMessage().getChatId(), "Failed to download news content.");
                }
            }
        }
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

    private void sendNewsUpdate(long chatId) {
        try {
            LOGGER.info("Sending news update to Chat ID: " + chatId);
            String htmlContent = WebContentDownloader.downloadContent();
            List<String> newsBlocks = extractNewsBlocks(htmlContent);

            for (String newsBlock : newsBlocks) {
                sendLongMessage(chatId, newsBlock);
            }
        } catch (IOException e) {
            LOGGER.severe("Error sending news update: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    private void checkForNewsUpdates() {
        // Logic to check and send updates...
        try {
            String currentNewsContent = WebContentDownloader.downloadContent();
            Set<String> currentNewsSet = new HashSet<>(extractNewsBlocks(currentNewsContent));

            // Determine new or changed news
            Set<String> newOrChangedNews = new HashSet<>(currentNewsSet);
            newOrChangedNews.removeAll(lastFetchedNews);

            if (!newOrChangedNews.isEmpty()) {
                // Send only new or changed news to subscribed users
                for (Long chatId : subscribedUsers) {
                    for (String newsItem : newOrChangedNews) {
                        sendLongMessage(chatId, newsItem);
                    }
                }
            }

            // Update the last fetched news
            lastFetchedNews = currentNewsSet;
        } catch (IOException e) {
            LOGGER.severe("Error checking news updates: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
