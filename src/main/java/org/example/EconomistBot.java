package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.util.HashSet;
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
        message.setParseMode("HTML"); // Use HTML parse mode
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendNewsUpdate(long chatId) {
        try {
            LOGGER.info("Sending news update to Chat ID: " + chatId);
            String htmlContent = WebContentDownloader.downloadContent();
            String preparedContent = prepareHtmlContent(htmlContent);
            sendLongMessage(chatId, preparedContent);
        } catch (IOException e) {
            LOGGER.severe("Error sending news update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Prepares HTML content for sending to Telegram.
     * @param htmlContent The HTML content to be prepared.
     * @return The content formatted for Telegram HTML.
     */
    private String prepareHtmlContent(String htmlContent) {
        // This method should take the HTML content and make sure it's ready
        // to be sent as an HTML message to Telegram.
        // It should handle necessary HTML tag conversions if needed and ensure
        // that the HTML is safe to send and display.

        // For now, let's assume the content is ready and does not require further processing.
        // You may need to implement additional logic depending on your HTML content.
        return htmlContent;
    }
}
