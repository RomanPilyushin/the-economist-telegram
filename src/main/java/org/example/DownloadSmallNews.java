package org.example;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class DownloadSmallNews {
    private static final Logger LOGGER = Logger.getLogger(DownloadSmallNews.class.getName());
    private static final Map<String, String> lastFetchedHeaders = new HashMap<>();

    public static void main(String[] args) {
        try {
            LOGGER.info("Starting WebContentDownloader main method...");
            String content = downloadContent();
            if (content != null && !content.isEmpty()) {
                LOGGER.info("Content downloaded successfully.");
                saveToFile(content, "extractedHtmlContent.html");
            } else {
                LOGGER.warning("No content fetched or markers not found.");
            }
        } catch (IOException e) {
            LOGGER.severe("Error in downloading content: " + e.getMessage());
        }
    }

    public static String downloadContent() throws IOException {
        LOGGER.info("Starting content download...");
        String url = "https://www.economist.com/the-world-in-brief?nocache=" + System.currentTimeMillis();

        Connection connection = Jsoup.connect(url)
                .header("Content-Type", "text/html; charset=utf-8")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0");

        // Use last-modified and etag headers for conditional requests
        if (lastFetchedHeaders.containsKey("Last-Modified")) {
            connection.header("If-Modified-Since", lastFetchedHeaders.get("Last-Modified"));
        }
        if (lastFetchedHeaders.containsKey("ETag")) {
            connection.header("If-None-Match", lastFetchedHeaders.get("ETag"));
        }

        Connection.Response response = connection.execute();
        updateLastFetchedHeaders(response);

        // Check for 304 Not Modified
        if (response.statusCode() == 304) {
            LOGGER.info("Content not modified since last fetch.");
            return null; // Content not modified
        }

        Document document = response.parse();
        Elements gobbetElements = document.select("div._gobbet");
        StringBuilder extractedContent = new StringBuilder();

        for (Element gobbet : gobbetElements) {
            extractedContent.append(gobbet.outerHtml());
        }

        if (extractedContent.length() == 0) {
            LOGGER.warning("No content extracted from the gobbet sections.");
            return null;
        }

        return extractedContent.toString();
    }

    private static void updateLastFetchedHeaders(Connection.Response response) {
        String lastModified = response.header("Last-Modified");
        String eTag = response.header("ETag");

        if (lastModified != null) {
            lastFetchedHeaders.put("Last-Modified", lastModified);
        }
        if (eTag != null) {
            lastFetchedHeaders.put("ETag", eTag);
        }
    }

    static void saveToFile(String content, String fileName) throws IOException {
        if (content != null && !content.isEmpty()) {
            Files.write(Paths.get(fileName), content.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("Saved content to file: " + fileName);
        } else {
            LOGGER.warning("No content to save to file.");
        }
    }
}
