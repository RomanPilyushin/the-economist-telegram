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

public class DownloadBigNews {
    private static final Logger LOGGER = Logger.getLogger(DownloadSmallNews.class.getName());

    public static void main(String[] args) {
        try {
            LOGGER.info("Starting WebContentDownloader main method...");
            String content = downloadContent();
            if (content != null && !content.isEmpty()) {
                LOGGER.info("Content downloaded successfully.");
                saveToFile(content, "extractedBigHtmlContent.html");
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
        Document document = Jsoup.connect(url).header("Content-Type", "text/html; charset=utf-8").get();
        LOGGER.info("Connected to URL: " + url);

        // Select all article elements
        Elements articleElements = document.select("div._article");
        StringBuilder extractedContent = new StringBuilder();

        for (Element article : articleElements) {
            // Extract the headline
            String headline = article.select("h3._headline").text();

            // Extract the main image URL
            String imageUrl = article.select("figure._main-image img").attr("src");

            // Start formatting the news block
            extractedContent.append("<div class='news-block'>")
                    .append("<h2>").append(headline).append("</h2>")
                    .append("<img src='").append(imageUrl).append("' alt='News Image'>");

            // Extract and append each paragraph individually
            Elements paragraphs = article.select("div._content p");
            for (Element paragraph : paragraphs) {
                extractedContent.append("<p>").append(paragraph.text()).append("</p>");
            }

            // Close the news block
            extractedContent.append("</div>\n");
        }

        // Check if any content was extracted
        if (extractedContent.length() == 0) {
            LOGGER.warning("No content extracted from the article sections.");
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
