package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class WebContentDownloader {
    private static final Logger LOGGER = Logger.getLogger(WebContentDownloader.class.getName());
    /*
    public static void main(String[] args) {
        try {
            LOGGER.info("Starting WebContentDownloader main method...");
            String content = downloadContent();
            if (content != null) {
                LOGGER.info("Content downloaded successfully.");
                saveToFile(content, "extractedHtmlContent.html");
            } else {
                LOGGER.warning("No content fetched.");
            }
        } catch (IOException e) {
            LOGGER.severe("Error in downloading content: " + e.getMessage());
        }
    }
    */
    public static String downloadContent() throws IOException {
        LOGGER.info("Starting content download...");
        String url = "https://www.economist.com/the-world-in-brief";
        Document document = Jsoup.connect(url).header("Content-Type", "text/html; charset=utf-8").get();
        LOGGER.info("Connected to URL: " + url);

        // Your logic to find the start and end markers can remain the same
        String startMarker = "<section class=\"css-1w4nt3t e1mdtgh40\">";
        String endMarker = "<h3 class=\"_headline\">Daily quiz</h3>";
        // ... rest of your code to find start and end index

        // Assuming extractedContent is the HTML you want to send
        String extractedContent = extractContentBetweenMarkers(document, startMarker, endMarker);

        // Remove unwanted elements
        Document contentDoc = Jsoup.parse(extractedContent);
        contentDoc.select("figcaption, img").remove();

        // Save the HTML content to a file for checking
        saveToFile(contentDoc.html(), "extractedHtmlContent.html");

        // Return the clean HTML content
        return contentDoc.html();
    }

    // Helper method to extract content between markers
    private static String extractContentBetweenMarkers(Document document, String startMarker, String endMarker) {
        String bodyHtml = document.select("body").html();
        int startIndex = bodyHtml.indexOf(startMarker);
        int endIndex = bodyHtml.indexOf(endMarker, startIndex);
        if (startIndex != -1 && endIndex != -1) {
            return bodyHtml.substring(startIndex, endIndex);
        }
        return null;
    }

    private static void saveToFile(String content, String fileName) throws IOException {
        Files.write(Paths.get(fileName), content.getBytes(StandardCharsets.UTF_8));
        LOGGER.info("Saved content to file: " + fileName);
    }
}
