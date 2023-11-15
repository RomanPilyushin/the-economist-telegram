package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import java.io.FileOutputStream;

public class WebContentDownloader {

    public static void main(String[] args) {
        try {
            String content = downloadContent();
            if (content != null) {
                System.out.println(content);
            } else {
                System.out.println("No content fetched.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String downloadContent() throws IOException {
        String url = "https://www.economist.com/the-world-in-brief";
        Document document = Jsoup.connect(url).get();
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

    private static void saveToFile(String content, String fileName) throws IOException {
        try (FileOutputStream out = new FileOutputStream(fileName)) {
            out.write(content.getBytes());
        }
    }
}
