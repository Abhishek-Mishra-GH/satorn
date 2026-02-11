package com._xdev.satorn.service.feed;

import com._xdev.satorn.domain.entity.*;
import com._xdev.satorn.domain.repository.*;
import com._xdev.satorn.service.ai.*;
import com._xdev.satorn.service.external.ArticleScrapingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * RSS Feed Monitoring Service
 * Monitors RSS feeds, scrapes articles, and generates synthesized versions
 * Implements rate limiting to respect API quotas
 */
@Slf4j
@Service
@Transactional
@ConditionalOnProperty(name = "rss.monitoring.auto-run-enabled", havingValue = "true")
public class RssFeedMonitoringService {

  private final RssFeedConfigRepository rssFeedConfigRepository;
  private final SynthesizedArticleRepository synthesizedArticleRepository;
  private final CategoryRepository categoryRepository;
  private final ArticleScrapingService articleScrapingService;
  private final ClaimExtractionService claimExtractionService;
  private final VerificationService verificationService;
  private final SynthesisService synthesisService;
  private final CategoryTaggingService categoryTaggingService;
  private final RateLimiter rateLimiter;
  private final RssPreVerificationQueueService rssPreVerificationQueueService;

  public RssFeedMonitoringService(
      RssFeedConfigRepository rssFeedConfigRepository,
      SynthesizedArticleRepository synthesizedArticleRepository,
      CategoryRepository categoryRepository,
      ArticleScrapingService articleScrapingService,
      ClaimExtractionService claimExtractionService,
      VerificationService verificationService,
      SynthesisService synthesisService,
      CategoryTaggingService categoryTaggingService,
      RateLimiter rateLimiter,
      RssPreVerificationQueueService rssPreVerificationQueueService) {
    this.rssFeedConfigRepository = rssFeedConfigRepository;
    this.synthesizedArticleRepository = synthesizedArticleRepository;
    this.categoryRepository = categoryRepository;
    this.articleScrapingService = articleScrapingService;
    this.claimExtractionService = claimExtractionService;
    this.verificationService = verificationService;
    this.synthesisService = synthesisService;
    this.categoryTaggingService = categoryTaggingService;
    this.rateLimiter = rateLimiter;
    this.rssPreVerificationQueueService = rssPreVerificationQueueService;

    // Initialize rate limiters
    rateLimiter.initializeProvider("groq", 29); // 29 requests per minute
    rateLimiter.initializeProvider("openai", 60); // 60 requests per minute (conservative)
  }

  /**
   * Main scheduled task that monitors all RSS feeds
   * Runs every 5 minutes by default
   */
  @Scheduled(fixedDelayString = "${rss.monitoring.interval:300000}", initialDelayString = "${rss.monitoring.initial-delay:60000}")
  public void monitorRssFeeds() {
    try {
      Map<String, Object> result = rssPreVerificationQueueService.enqueueEligibleFeeds(false);
      log.info("RSS discovery cycle completed: {}", result);
    } catch (DataAccessException e) {
      log.warn("Skipping RSS discovery cycle: RSS tables not available yet ({})", e.getMostSpecificCause().getMessage());
    } catch (Exception e) {
      log.error("Unexpected error in RSS discovery cycle", e);
    }
  }

  /**
   * Queue worker scheduler with bounded batch size and retry/backoff.
   */
  @Scheduled(
      fixedDelayString = "${rss.monitoring.processor-interval:20000}",
      initialDelayString = "${rss.monitoring.processor-initial-delay:90000}")
  public void processPreVerificationQueue() {
    try {
      Map<String, Object> result = rssPreVerificationQueueService.processQueueBatch();
      if (Boolean.FALSE.equals(result.get("busy"))) {
        log.info("RSS queue drain cycle completed: {}", result);
      }
    } catch (DataAccessException e) {
      log.warn("Skipping RSS queue drain cycle: RSS tables not available yet ({})", e.getMostSpecificCause().getMessage());
    } catch (Exception e) {
      log.error("Unexpected error in RSS queue drain cycle", e);
    }
  }

  /**
   * Check if feed should be processed based on last check time
   */
  private boolean shouldCheckFeed(RssFeedConfig feed) {
    if (feed.getLastChecked() == null) {
      return true;
    }

    LocalDateTime nextCheckTime = feed.getLastChecked()
        .plusMinutes(feed.getUpdateFrequencyMinutes());

    return LocalDateTime.now().isAfter(nextCheckTime);
  }

  /**
   * Process individual RSS feed
   */
  private void processFeed(RssFeedConfig feed) throws Exception {
    log.info("Processing feed: {} from {}", feed.getName(), feed.getFeedUrl());

    List<RssFeedItem> items = parseRssFeed(feed.getFeedUrl());
    int processed = 0;

    for (RssFeedItem item : items) {
      // Check rate limiting for Groq (used in claim extraction)
      if (!rateLimiter.allowRequest("groq")) {
        log.warn("Rate limit reached for Groq, waiting before processing next article");
        try {
          rateLimiter.waitUntilAllowed("groq");
        } catch (InterruptedException e) {
          log.error("Interrupted while waiting for rate limit", e);
          Thread.currentThread().interrupt();
          break;
        }
      }

      try {
        // Check if article already processed
        if (synthesizedArticleRepository.findBySourceUrl(item.link) != null) {
          log.debug("Article already processed: {}", item.link);
          continue;
        }

        // Scrape and synthesize article
        SynthesizedArticle synthesized = scrapeAndSynthesizeArticle(item, feed);

        if (synthesized != null) {
          synthesizedArticleRepository.save(synthesized);
          processed++;
          log.info("Synthesized article: {}", synthesized.getTitle());
        }
      } catch (Exception e) {
        log.error("Error processing article from feed: {}", item.link, e);
      }
    }

    // Update feed stats
    feed.setLastChecked(LocalDateTime.now());
    feed.setArticlesProcessed(feed.getArticlesProcessed() + processed);
    feed.setConsecutiveFailures(0);
    feed.setLastError(null);
    rssFeedConfigRepository.save(feed);

    log.info("Feed {} processing complete: {} articles processed", feed.getName(), processed);
  }

  /**
   * Scrape article content and generate synthesis
   */
  private SynthesizedArticle scrapeAndSynthesizeArticle(RssFeedItem item, RssFeedConfig feed) {
    try {
      log.info("Scraping article: {}", item.link);

      // Scrape article content
      ArticleScrapingService.ScrapedArticle scraped = articleScrapingService.scrapeArticle(item.link);

      if (scraped == null || scraped.getContent() == null || scraped.getContent().isEmpty()) {
        log.warn("Could not scrape content from: {}", item.link);
        return null;
      }

      // Extract claims
      List<Claim> claims = claimExtractionService.extractHighPriorityClaims(scraped.getContent(), 5);

      if (claims.isEmpty()) {
        log.debug("No claims extracted from: {}", item.link);
        return null;
      }

      // Verify claims
      List<Verification> verifications = verificationService.verifyMultipleClaims(
          claims.stream().map(Claim::getText).toList());

      // Get category
      CategoryTaggingService.CategoryTaggingResult categoryResult = categoryTaggingService
          .categorizeArticle(scraped.getTitle(), scraped.getContent());

      Category category = categoryRepository.findByName(categoryResult.getPrimaryCategory().getDisplayName())
          .orElseGet(() -> {
            Category newCategory = new Category();
            newCategory.setName(categoryResult.getPrimaryCategory().getDisplayName());
            newCategory.setColor(categoryResult.getPrimaryCategory().getColor());
            return categoryRepository.save(newCategory);
          });

      // Calculate statistics
      long trueClaims = verifications.stream()
          .filter(v -> "TRUE".equals(v.getVerdict()))
          .count();
      long falseClaims = verifications.stream()
          .filter(v -> "FALSE".equals(v.getVerdict()))
          .count();
      long unverifiable = verifications.stream()
          .filter(v -> "UNVERIFIABLE".equals(v.getVerdict()))
          .count();

      // Generate synthesis
      String narrative = generateNarrative(scraped, verifications);
      String timeline = extractTimeline(scraped.getContent());
      String keyFindings = generateKeyFindings(claims, verifications);

      double credibilityScore = calculateCredibilityScore(verifications);
      String verdict = determineVerdict(trueClaims, falseClaims, unverifiable);

      // Build synthesized article
      SynthesizedArticle synthesized = SynthesizedArticle.builder()
          .title(scraped.getTitle())
          .originalContent(scraped.getContent())
          .synthesizedNarrative(narrative)
          .sourceUrl(item.link)
          .originalSource(item.link)
          .rssFeedSource(feed.getName())
          .author(scraped.getAuthor() != null ? scraped.getAuthor() : "Unknown")
          .imageUrl(scraped.getImageUrl())
          .publishDate(item.pubDate)
          .credibilityScore(credibilityScore)
          .status("VERIFIED")
          .verdict(verdict)
          .category(category)
          .keyFindings(keyFindings)
          .timeline(timeline)
          .claimsCount(claims.size())
          .verifiedClaimsCount(verifications.size())
          .trueClaims((int) trueClaims)
          .falseClaims((int) falseClaims)
          .unverifiableClaims((int) unverifiable)
          .viewCount(0L)
          .isTrending(credibilityScore > 75.0 || trueClaims > 3) // Mark as trending if high credibility
          .build();

      return synthesized;
    } catch (Exception e) {
      log.error("Error scraping and synthesizing article: {}", item.link, e);
      return null;
    }
  }

  /**
   * Parse RSS feed and extract items
   */
  private List<RssFeedItem> parseRssFeed(String feedUrl) throws Exception {
    List<RssFeedItem> items = new ArrayList<>();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(new URL(feedUrl).openStream());

    // Parse RSS items
    NodeList itemNodes = doc.getElementsByTagName("item");

    for (int i = 0; i < Math.min(itemNodes.getLength(), 10); i++) { // Limit to 10 items per feed
      Element itemElement = (Element) itemNodes.item(i);

      String title = getElementValue(itemElement, "title");
      String link = getElementValue(itemElement, "link");
      String description = getElementValue(itemElement, "description");
      String pubDateStr = getElementValue(itemElement, "pubDate");

      LocalDateTime pubDate = parsePubDate(pubDateStr);

      if (link != null && !link.isEmpty()) {
        items.add(new RssFeedItem(title, link, description, pubDate));
      }
    }

    log.info("Parsed {} items from RSS feed: {}", items.size(), feedUrl);
    return items;
  }

  /**
   * Get element value from XML element
   */
  private String getElementValue(Element element, String tagName) {
    NodeList nodeList = element.getElementsByTagName(tagName);
    if (nodeList.getLength() > 0) {
      return nodeList.item(0).getTextContent();
    }
    return null;
  }

  /**
   * Parse publication date from RSS format
   */
  private LocalDateTime parsePubDate(String pubDateStr) {
    if (pubDateStr == null || pubDateStr.isEmpty()) {
      return LocalDateTime.now();
    }

    try {
      // Try RFC 2822 format (most common in RSS)
      ZonedDateTime zdt = ZonedDateTime.parse(pubDateStr, DateTimeFormatter.RFC_1123_DATE_TIME);
      return zdt.toLocalDateTime();
    } catch (Exception e) {
      log.debug("Could not parse date: {}, using current time", pubDateStr);
      return LocalDateTime.now();
    }
  }

  /**
   * Generate narrative from article and verifications
   */
  private String generateNarrative(ArticleScrapingService.ScrapedArticle article,
      List<Verification> verifications) {
    StringBuilder narrative = new StringBuilder();

    narrative.append("Summary: ")
        .append(article.getDescription() != null ? article.getDescription() : article.getTitle())
        .append("\n\n");

    narrative.append("Key Claims Verified:\n");

    int trueClaims = 0;
    int falseClaims = 0;

    for (Verification v : verifications) {
      if ("TRUE".equals(v.getVerdict())) {
        trueClaims++;
      } else if ("FALSE".equals(v.getVerdict())) {
        falseClaims++;
      }
    }

    narrative.append("- ").append(trueClaims).append(" claims verified as TRUE\n");
    narrative.append("- ").append(falseClaims).append(" claims found to be FALSE\n");
    narrative.append("- ").append(verifications.size() - trueClaims - falseClaims)
        .append(" claims are UNVERIFIABLE\n\n");

    narrative.append("Original article: ").append(article.getTitle());

    return narrative.toString();
  }

  /**
   * Extract timeline from article content
   */
  private String extractTimeline(String content) {
    // This is a simplified version; could be enhanced with TimelineBuilderService
    return "Timeline information extracted from article content. " +
        "For detailed timeline, see the full article.";
  }

  /**
   * Generate key findings
   */
  private String generateKeyFindings(List<Claim> claims, List<Verification> verifications) {
    StringBuilder findings = new StringBuilder();

    findings.append("Key Findings:\n");
    findings.append("- ").append(claims.size()).append(" major claims identified\n");

    int verified = (int) verifications.stream().filter(v -> !"UNVERIFIABLE".equals(v.getVerdict())).count();
    findings.append("- ").append(verified).append(" out of ").append(verifications.size())
        .append(" claims could be verified\n");

    long trueCount = verifications.stream().filter(v -> "TRUE".equals(v.getVerdict())).count();
    findings.append("- ").append(trueCount).append(" claims confirmed as accurate\n");

    return findings.toString();
  }

  /**
   * Calculate overall credibility score
   */
  private double calculateCredibilityScore(List<Verification> verifications) {
    if (verifications.isEmpty()) {
      return 50.0;
    }

    double totalScore = 0;
    for (Verification v : verifications) {
      totalScore += v.getConfidence() != null ? v.getConfidence() : 0.5;
    }

    return Math.min(100.0, (totalScore / verifications.size()) * 100);
  }

  /**
   * Determine verdict based on claims
   */
  private String determineVerdict(long trueClaims, long falseClaims, long unverifiable) {
    if (falseClaims > trueClaims) {
      return "MOSTLY_FALSE";
    } else if (trueClaims > falseClaims * 2) {
      return "MOSTLY_TRUE";
    } else if (trueClaims > 0 && falseClaims > 0) {
      return "MIXED";
    } else if (unverifiable > trueClaims + falseClaims) {
      return "UNVERIFIABLE";
    }
    return "MIXED";
  }

  /**
   * Handle feed processing errors
   */
  private void handleFeedError(RssFeedConfig feed, Exception e) {
    feed.setConsecutiveFailures(feed.getConsecutiveFailures() + 1);
    feed.setLastError(e.getMessage());

    // Disable feed after 5 consecutive failures
    if (feed.getConsecutiveFailures() > 5) {
      feed.setEnabled(false);
      log.warn("Disabled feed {} after {} consecutive failures", feed.getName(), feed.getConsecutiveFailures());
    }

    rssFeedConfigRepository.save(feed);
  }

  /**
   * Inner class for RSS feed items
   */
  public static class RssFeedItem {
    public String title;
    public String link;
    public String description;
    public LocalDateTime pubDate;

    public RssFeedItem(String title, String link, String description, LocalDateTime pubDate) {
      this.title = title;
      this.link = link;
      this.description = description;
      this.pubDate = pubDate;
    }
  }
}
