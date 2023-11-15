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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


public class EconomistBot extends TelegramLongPollingBot {
    private static final int MAX_MESSAGE_LENGTH = 4096; // Maximum length of a Telegram message

    private static final Logger LOGGER = Logger.getLogger(EconomistBot.class.getName());
    private static final Set<Long> subscribedUsers = new HashSet<>();
    private static final String BOT_TOKEN = "6530358402:AAG28dxK3SQCjlPIYayi-aysMNAI8oOZcyw";
    private static final String BOT_USERNAME = "https://t.me/Theworldinbrief_bot";

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

    private String prepareHtmlContent(String htmlContent) {
        // Parse the HTML content
        Document document = Jsoup.parse(htmlContent);

        // Remove all script and style elements
        document.select("script, style").forEach(Element::remove);

        // Extract plain text from the document
        String plainText = document.text();

        // Insert newline characters to format the text
        // This is a basic example and may need to be adjusted based on actual text patterns
        String formattedText = plainText.replaceAll("(\\. )", ".\n\n")
                .replaceAll("(\\? )", "?\n\n")
                .replaceAll("(\\! )", "!\n\n");

        LOGGER.info("Formatted Plain Text: " + formattedText); // Log the formatted text
        return formattedText;
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

        Elements potentialNewsElements = document.select("p, div._article.ds-layout-grid");

        for (Element element : potentialNewsElements) {
            StringBuilder newsText = new StringBuilder();

            if (element.tagName().equals("div") && element.hasClass("_article ds-layout-grid")) {
                // Extract the headline if available
                String headline = element.select("h3._headline").text();
                if (!headline.isEmpty()) {
                    newsText.append(headline).append("\n\n");
                }

                // Add the text from each paragraph within the div
                Elements paragraphs = element.select("p");
                for (Element paragraph : paragraphs) {
                    newsText.append(paragraph.text()).append("\n\n");
                }
            } else if (element.tagName().equals("p")) {
                // If it's a standalone <p> tag, add its text
                newsText.append(element.text());
            }

            if (!newsText.toString().trim().isEmpty()) {
                newsBlocks.add(newsText.toString().trim());
            }
        }

        return newsBlocks;
    }

    private void sendNewsUpdate(long chatId) {
        try {
            LOGGER.info("Sending news update to Chat ID: " + chatId);
            String htmlContent = WebContentDownloader.downloadContent();
            LOGGER.info("Downloaded Content: " + htmlContent);

            List<String> newsBlocks = extractNewsBlocks(htmlContent);
            for (String newsBlock : newsBlocks) {
                sendLongMessage(chatId, newsBlock);
            }
        } catch (IOException e) {
            LOGGER.severe("Error sending news update: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
