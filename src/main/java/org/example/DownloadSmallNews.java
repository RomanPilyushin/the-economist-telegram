package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class DownloadSmallNews {
    private static final Logger LOGGER = Logger.getLogger(DownloadSmallNews.class.getName());

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
        String url = "https://www.economist.com/the-world-in-brief";
        Document document = Jsoup.connect(url)
                .header("Content-Type", "text/html; charset=utf-8")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .get();
        LOGGER.info("Connected to URL: " + url);

        // Select all elements with the class "_gobbet"
        Elements gobbetElements = document.select("div._gobbet");
        StringBuilder extractedContent = new StringBuilder();

        for (Element gobbet : gobbetElements) {
            extractedContent.append(gobbet.outerHtml()); // Appends the entire HTML of each gobbet section
        }

        // Check if any content was extracted
        if (extractedContent.length() == 0) {
            LOGGER.warning("No content extracted from the gobbet sections.");
            return null;
        }

        // Return the extracted HTML content
        return extractedContent.toString();
    }

    private static void saveToFile(String content, String fileName) throws IOException {
        if (content != null && !content.isEmpty()) {
            Files.write(Paths.get(fileName), content.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("Saved content to file: " + fileName);
        } else {
            LOGGER.warning("No content to save to file.");
        }
    }
}
