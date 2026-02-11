package com._xdev.satorn.service.external;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Proxy;
import java.util.Date;

/**
 * Service for scraping article content from URLs
 */
@Slf4j
@Service
public class ArticleScrapingService {

    private static final int CONNECTION_TIMEOUT = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
    @Value("${network.bypass-system-proxy:true}")
    private boolean bypassSystemProxy;

    /**
     * Scrape article from URL
     */
    public ScrapedArticle scrapeArticle(String url) {
        try {
            log.info("Scraping article from URL: {}", url);

            Document doc = newConnection(url, CONNECTION_TIMEOUT).get();

            ScrapedArticle article = new ScrapedArticle();
            article.setSourceUrl(url);
            article.setTitle(extractTitle(doc));
            article.setContent(extractContent(doc));
            article.setAuthor(extractAuthor(doc));
            article.setDescription(extractDescription(doc));
            article.setImageUrl(extractImageUrl(doc));
            article.setDomain(extractDomain(url));
            article.setPublishedDate(new Date());

            log.info("Successfully scraped article: {}", article.getTitle());
            return article;
        } catch (IOException e) {
            log.error("Failed to scrape article from URL: {}", url, e);
            return null;
        }
    }

    private String extractTitle(Document doc) {
        String[] titleSelectors = { "h1", "meta[property=og:title]", "title", ".headline" };

        for (String selector : titleSelectors) {
            if (selector.startsWith("meta")) {
                Element meta = doc.selectFirst(selector);
                if (meta != null) {
                    String content = meta.attr("content");
                    if (content != null && !content.isEmpty()) {
                        return content;
                    }
                }
            } else {
                Element element = doc.selectFirst(selector);
                if (element != null) {
                    String text = element.text();
                    if (text != null && !text.isEmpty()) {
                        return text;
                    }
                }
            }
        }

        return "Untitled Article";
    }

    private String extractContent(Document doc) {
        doc.select("script").remove();
        doc.select("style").remove();

        String[] contentSelectors = {
                "article",
                ".article-body",
                ".post-content",
                ".entry-content",
                ".content",
                "main"
        };

        for (String selector : contentSelectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String text = element.text();
                if (text.length() > 100) {
                    return text;
                }
            }
        }

        StringBuilder content = new StringBuilder();
        for (Element p : doc.select("p")) {
            content.append(p.text()).append("\n");
        }

        return content.toString();
    }

    private String extractAuthor(Document doc) {
        Element meta = doc.selectFirst("meta[name=author]");
        if (meta != null) {
            return meta.attr("content");
        }
        return "Unknown Author";
    }

    private String extractDescription(Document doc) {
        Element meta = doc.selectFirst("meta[name=description]");
        if (meta != null) {
            return meta.attr("content");
        }
        return "";
    }

    private String extractImageUrl(Document doc) {
        Element meta = doc.selectFirst("meta[property=og:image]");
        if (meta != null) {
            return meta.attr("content");
        }
        return "";
    }

    private String extractDomain(String url) {
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Check if URL is accessible
     */
    public boolean isUrlAccessible(String url) {
        try {
            // Jsoup no longer exposes a head() shortcut on Connection in recent versions;
            // use execute() with GET and ignore the body for reachability checks. [web:23][web:31]
            newConnection(url, 5000)
                    .method(org.jsoup.Connection.Method.GET)
                    .execute();

            return true;
        } catch (Exception e) {
            log.warn("URL not accessible: {}", url, e);
            return false;
        }
    }

    private Connection newConnection(String url, int timeoutMs) {
        Connection connection = Jsoup.connect(url)
                .timeout(timeoutMs)
                .userAgent(USER_AGENT);
        if (bypassSystemProxy) {
            connection.proxy(Proxy.NO_PROXY);
        }
        return connection;
    }

    @lombok.Data
    public static class ScrapedArticle {
        private String sourceUrl;
        private String title;
        private String content;
        private Date publishedDate;
        private String author;
        private String description;
        private String imageUrl;
        private String domain;
    }
}
