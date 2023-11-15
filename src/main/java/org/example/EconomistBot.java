package org.example;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class EconomistBot {
    private static final Logger LOGGER = Logger.getLogger(EconomistBot.class.getName());
    private static final Set<Long> subscribedUsers = new HashSet<>();
    private static final ZoneId KYIV_ZONE_ID = ZoneId.of("Europe/Kiev");
    private static final String BOT_TOKEN = "6530358402:AAG28dxK3SQCjlPIYayi-aysMNAI8oOZcyw";

    public static void main(String[] args) {
        TelegramBot bot = new TelegramBot(BOT_TOKEN);

        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                LOGGER.info("Received update: " + update);
                if (update.message() != null && "/start".equals(update.message().text())) {
                    long chatId = update.message().chat().id();
                    subscribedUsers.add(chatId);
                    LOGGER.info("User subscribed. Chat ID: " + chatId);
                    sendNewsUpdate(bot, chatId);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        scheduleDailyTask(bot);
    }

    private static void scheduleDailyTask(TelegramBot bot) {
        ZonedDateTime now = ZonedDateTime.now(KYIV_ZONE_ID);
        ZonedDateTime nextRun = now.withHour(9).withMinute(0).withSecond(0);
        if (now.compareTo(nextRun) > 0) {
            nextRun = nextRun.plusDays(1);
        }

        long delay = Duration.between(now, nextRun).getSeconds();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> sendNewsUpdateToAllSubscribers(bot),
                delay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
    }

    private static void sendNewsUpdateToAllSubscribers(TelegramBot bot) {
        for (long chatId : subscribedUsers) {
            sendNewsUpdate(bot, chatId);
        }
    }

    private static void sendNewsUpdate(TelegramBot bot, long chatId) {
        try {
            LOGGER.info("Sending news update to Chat ID: " + chatId);
            String formattedContent = WebContentDownloader.downloadContent();
            bot.execute(new SendMessage(chatId, formattedContent).parseMode(ParseMode.HTML));
        } catch (Exception e) {
            LOGGER.severe("Error sending news update: " + e.getMessage());
        }
    }
}
