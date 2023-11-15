package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class WebContentDownloader {

    private static final Logger LOGGER = Logger.getLogger(WebContentDownloader.class.getName());

    public static void main(String[] args) {
        try {
            LOGGER.info("Starting WebContentDownloader main method...");
            String content = downloadContent();
            if (content != null) {
                LOGGER.info("Content downloaded successfully.");
                // LOGGER.info(content); // Uncomment if you want to log the content
            } else {
                LOGGER.warning("No content fetched.");
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
        String bodyHtml = document.select("body").html();

        String startMarker = "<section class=\"css-1w4nt3t e1mdtgh40\">";
        String endMarker = "<h3 class=\"_headline\">Daily quiz</h3>";

        int startIndex = bodyHtml.indexOf(startMarker);
        int endIndex = bodyHtml.indexOf(endMarker, startIndex);
        if (startIndex != -1 && endIndex != -1) {
            String extractedContent = bodyHtml.substring(startIndex, endIndex);
            Document contentDoc = Jsoup.parse(extractedContent);

            Elements figcaptions = contentDoc.select("figcaption.css-1xn38vl.e9xx8940");
            figcaptions.forEach(Element::remove);

            Elements images = contentDoc.select("img");
            images.forEach(Element::remove);

            // Save the HTML content to a file for checking
            saveToFile(contentDoc.html(), "extractedHtmlContent.html");

            // Convert HTML to Markdown or simplified HTML
            return convertToTelegramFormat(contentDoc);
        }
        return null;
    }

    private static String convertToTelegramFormat(Document document) {
        StringBuilder sb = new StringBuilder();
        for (Node node : document.body().childNodes()) {
            processNode(node, sb);
        }
        return sb.toString();
    }

    private static void processNode(Node node, StringBuilder sb) {
        if (node instanceof TextNode) {
            sb.append(((TextNode) node).text());
        } else if (node.nodeName().equals("b") || node.nodeName().equals("strong")) {
            sb.append("<b>").append(node.childNode(0).toString()).append("</b>");
        } else if (node.nodeName().equals("i") || node.nodeName().equals("em")) {
            sb.append("<i>").append(node.childNode(0).toString()).append("</i>");
        }
        // Add other formatting tags as needed

        for (Node child : node.childNodes()) {
            processNode(child, sb);
        }
    }

    private static void saveToFile(String content, String fileName) {
        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("Saved content to file: " + fileName);
        } catch (IOException e) {
            LOGGER.severe("Error saving content to file: " + e.getMessage());
        }
    }
}
