package com._xdev.satorn.service.feed;

import com._xdev.satorn.domain.entity.Article;
import com._xdev.satorn.domain.entity.Category;
import com._xdev.satorn.domain.entity.Claim;
import com._xdev.satorn.domain.entity.Evidence;
import com._xdev.satorn.domain.entity.RssFeedConfig;
import com._xdev.satorn.domain.entity.SynthesizedArticle;
import com._xdev.satorn.domain.entity.Synthesis;
import com._xdev.satorn.domain.entity.Verification;
import com._xdev.satorn.domain.repository.CategoryRepository;
import com._xdev.satorn.domain.repository.RssFeedConfigRepository;
import com._xdev.satorn.domain.repository.SynthesizedArticleRepository;
import com._xdev.satorn.service.ai.CategoryTaggingService;
import com._xdev.satorn.service.ai.ClaimExtractionService;
import com._xdev.satorn.service.ai.RagContextService;
import com._xdev.satorn.service.ai.RateLimiter;
import com._xdev.satorn.service.ai.SynthesisService;
import com._xdev.satorn.service.ai.TimelineBuilderService;
import com._xdev.satorn.service.ai.VerificationService;
import com._xdev.satorn.service.external.ArticleScrapingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

/**
 * Maintains a smart pre-verification queue for RSS items.
 * Discovery and verification are decoupled so low token budget does not halt intake.
 */
@Slf4j
@Service
public class RssPreVerificationQueueService {

  private static final String GROQ_PROVIDER = "groq";
  private static final String OPENAI_PROVIDER = "openai";

  private final RssFeedConfigRepository rssFeedConfigRepository;
  private final SynthesizedArticleRepository synthesizedArticleRepository;
  private final CategoryRepository categoryRepository;
  private final ArticleScrapingService articleScrapingService;
  private final ClaimExtractionService claimExtractionService;
  private final VerificationService verificationService;
  private final SynthesisService synthesisService;
  private final CategoryTaggingService categoryTaggingService;
  private final RagContextService ragContextService;
  private final RateLimiter rateLimiter;
  private final TimelineBuilderService timelineBuilderService;

  @Value("${rss.monitoring.intake-max-items-per-feed:150}")
  private int intakeMaxItemsPerFeed;

  @Value("${rss.monitoring.enqueue-limit-per-feed:15}")
  private int enqueueLimitPerFeed;

  @Value("${rss.monitoring.queue-capacity:500}")
  private int queueCapacity;

  @Value("${rss.monitoring.process-batch-size:3}")
  private int processBatchSize;

  @Value("${rss.monitoring.max-retries:4}")
  private int maxRetries;

  @Value("${rss.monitoring.retry-base-delay-ms:15000}")
  private long retryBaseDelayMs;

  @Value("${rss.monitoring.failed-url-cooldown-minutes:360}")
  private int failedUrlCooldownMinutes;

  @Value("${rss.monitoring.min-content-length:250}")
  private int minContentLength;

  @Value("${rss.monitoring.min-rss-summary-length:80}")
  private int minRssSummaryLength;

  @Value("${network.bypass-system-proxy:true}")
  private boolean bypassSystemProxy;

  @Value("${rss.monitoring.max-claims-per-article:4}")
  private int maxClaimsPerArticle;

  @Value("${rss.monitoring.manual-run-max-articles:100}")
  private int manualRunMaxArticles;

  private final PriorityBlockingQueue<QueuedCandidate> queue = new PriorityBlockingQueue<>();
  private final Set<String> queuedUrls = ConcurrentHashMap.newKeySet();
  private final Map<String, Instant> cooldownUrls = new ConcurrentHashMap<>();
  private final AtomicBoolean processing = new AtomicBoolean(false);
  private final AtomicLong sequence = new AtomicLong(0);
  private final AtomicLong totalEnqueued = new AtomicLong(0);
  private final AtomicLong totalProcessed = new AtomicLong(0);
  private final AtomicLong totalDropped = new AtomicLong(0);
  private final AtomicLong totalRateLimited = new AtomicLong(0);
  private final AtomicBoolean manualRunInProgress = new AtomicBoolean(false);
  private volatile Map<String, Object> manualRunStatus = Map.of("status", "IDLE");
  private final Deque<ManualRunSnapshot> manualRunHistory = new ConcurrentLinkedDeque<>();

  public RssPreVerificationQueueService(
      RssFeedConfigRepository rssFeedConfigRepository,
      SynthesizedArticleRepository synthesizedArticleRepository,
      CategoryRepository categoryRepository,
      ArticleScrapingService articleScrapingService,
      ClaimExtractionService claimExtractionService,
      VerificationService verificationService,
      SynthesisService synthesisService,
      CategoryTaggingService categoryTaggingService,
      RagContextService ragContextService,
      RateLimiter rateLimiter,
      TimelineBuilderService timelineBuilderService) {
    this.rssFeedConfigRepository = rssFeedConfigRepository;
    this.synthesizedArticleRepository = synthesizedArticleRepository;
    this.categoryRepository = categoryRepository;
    this.articleScrapingService = articleScrapingService;
    this.claimExtractionService = claimExtractionService;
    this.verificationService = verificationService;
    this.synthesisService = synthesisService;
    this.categoryTaggingService = categoryTaggingService;
    this.ragContextService = ragContextService;
    this.rateLimiter = rateLimiter;
    this.timelineBuilderService = timelineBuilderService;

    rateLimiter.initializeProvider(GROQ_PROVIDER, 29);
    rateLimiter.initializeProvider(OPENAI_PROVIDER, 60);
  }

  public Map<String, Object> enqueueEligibleFeeds(boolean force) {
    List<RssFeedConfig> feeds = rssFeedConfigRepository.findByEnabled(true);
    int scanned = 0;
    int discovered = 0;
    int enqueued = 0;
    int skipped = 0;
    int duplicates = 0;

    for (RssFeedConfig feed : feeds) {
      FeedEnqueueResult result = enqueueFeedInternal(feed, force);
      if (!result.skippedByFrequency) {
        scanned++;
      }
      discovered += result.discovered;
      enqueued += result.enqueued;
      skipped += result.skipped;
      duplicates += result.duplicates;
    }

    Map<String, Object> response = new HashMap<>();
    response.put("enabledFeeds", feeds.size());
    response.put("scannedFeeds", scanned);
    response.put("discoveredCandidates", discovered);
    response.put("enqueuedCandidates", enqueued);
    response.put("skippedCandidates", skipped);
    response.put("duplicateOrAlreadyProcessed", duplicates);
    response.put("queueSize", queue.size());
    return response;
  }

  public Map<String, Object> enqueueFeed(Long feedId, boolean force) {
    RssFeedConfig feed = rssFeedConfigRepository.findById(feedId)
        .orElseThrow(() -> new IllegalArgumentException("RSS feed not found"));

    FeedEnqueueResult result = enqueueFeedInternal(feed, force);
    Map<String, Object> response = new HashMap<>();
    response.put("feedId", feedId);
    response.put("feedName", feed.getName());
    response.put("skippedByFrequency", result.skippedByFrequency);
    response.put("discoveredCandidates", result.discovered);
    response.put("enqueuedCandidates", result.enqueued);
    response.put("skippedCandidates", result.skipped);
    response.put("duplicateOrAlreadyProcessed", result.duplicates);
    response.put("queueSize", queue.size());
    return response;
  }

  public Map<String, Object> processQueueBatch() {
    return processQueueBatch(processBatchSize);
  }

  public Map<String, Object> processQueueBatch(int maxItems) {
    QueueDrainResult result = drainQueue(Math.max(1, maxItems));
    Map<String, Object> response = new HashMap<>();
    response.put("busy", result.busy);
    response.put("processed", result.processed);
    response.put("requeued", result.requeued);
    response.put("dropped", result.dropped);
    response.put("queueSize", queue.size());
    return response;
  }

  public Map<String, Object> getQueueStatus() {
    Map<String, Object> status = new HashMap<>();
    status.put("queueSize", queue.size());
    status.put("queueCapacity", queueCapacity);
    status.put("processing", processing.get());
    status.put("totalEnqueued", totalEnqueued.get());
    status.put("totalProcessed", totalProcessed.get());
    status.put("totalDropped", totalDropped.get());
    status.put("totalRateLimited", totalRateLimited.get());
    status.put("rateLimiter", rateLimiter.getStatus());
    status.put("manualRun", manualRunStatus);
    return status;
  }

  public Map<String, Object> probeFeed(Long feedId) {
    RssFeedConfig feed = rssFeedConfigRepository.findById(feedId)
        .orElseThrow(() -> new IllegalArgumentException("RSS feed not found"));
    Instant startedAt = Instant.now();
    Map<String, Object> result = new HashMap<>();
    result.put("feedId", feedId);
    result.put("feedName", feed.getName());
    result.put("feedUrl", feed.getFeedUrl());
    try {
      int probeMax = Math.min(20, Math.max(1, intakeMaxItemsPerFeed));
      List<FeedItem> items = parseFeed(feed.getFeedUrl(), probeMax);
      List<Map<String, Object>> sample = items.stream()
          .limit(5)
          .map(item -> Map.<String, Object>of(
              "title", item.title(),
              "link", item.link(),
              "pubDate", item.pubDate()))
          .toList();
      result.put("status", "OK");
      result.put("itemCount", items.size());
      result.put("sampleItems", sample);
    } catch (Exception e) {
      result.put("status", "ERROR");
      result.put("error", summarizeException(e));
    }
    result.put("durationMs", Duration.between(startedAt, Instant.now()).toMillis());
    return result;
  }

  public Map<String, Object> startManualMonitoringRun(int maxArticles, boolean forceEnqueue) {
    int requestedMaxArticles = Math.max(1, maxArticles);
    int safeMaxArticles = Math.min(requestedMaxArticles, Math.max(1, manualRunMaxArticles));
    if (!manualRunInProgress.compareAndSet(false, true)) {
      return Map.of(
          "accepted", false,
          "message", "A manual monitoring run is already in progress",
          "run", manualRunStatus);
    }

    String runId = UUID.randomUUID().toString();
    Instant startedAt = Instant.now();
    Map<String, Object> initialStatus = new HashMap<>();
    initialStatus.put("runId", runId);
    initialStatus.put("status", "RUNNING");
    initialStatus.put("stage", "INITIALIZING");
    initialStatus.put("startedAt", startedAt.toString());
    initialStatus.put("requestedMaxArticles", requestedMaxArticles);
    initialStatus.put("maxArticles", safeMaxArticles);
    initialStatus.put("processed", 0);
    initialStatus.put("requeued", 0);
    initialStatus.put("dropped", 0);
    initialStatus.put("queueSize", queue.size());
    initialStatus.put("progressPercent", 0);
    updateManualRunStatus(initialStatus);

    CompletableFuture.runAsync(() -> executeManualRun(runId, startedAt, safeMaxArticles, forceEnqueue));

    return Map.of(
        "accepted", true,
        "message", "Manual monitoring run started",
        "run", manualRunStatus);
  }

  public Map<String, Object> getManualRunStatus() {
    return manualRunStatus;
  }

  public List<Map<String, Object>> getManualRunHistory(int limit) {
    int safeLimit = Math.max(1, limit);
    return manualRunHistory.stream()
        .limit(safeLimit)
        .map(snapshot -> snapshot.status)
        .toList();
  }

  private FeedEnqueueResult enqueueFeedInternal(RssFeedConfig feed, boolean force) {
    if (!force && !shouldCheckFeed(feed)) {
      return FeedEnqueueResult.skippedByFrequency();
    }

    FeedEnqueueResult result = new FeedEnqueueResult();
    try {
      List<FeedItem> items = parseFeed(feed.getFeedUrl(), intakeMaxItemsPerFeed);
      if (items.isEmpty()) {
        log.warn("RSS feed returned no items: {} ({})", feed.getName(), feed.getFeedUrl());
      }
      List<QueuedCandidate> candidates = new ArrayList<>();

      for (FeedItem item : items) {
        String url = normalizeUrl(item.link);
        if (url == null || isInCooldown(url)) {
          result.skipped++;
          continue;
        }
        if (queuedUrls.contains(url) || synthesizedArticleRepository.findBySourceUrl(url) != null) {
          result.duplicates++;
          continue;
        }
        candidates.add(new QueuedCandidate(sequence.incrementAndGet(), feed.getId(), feed.getName(),
            url, item.title, item.description, item.pubDate, scoreItem(item, feed), Instant.now(), 0));
      }

      candidates.sort(Comparator.comparingDouble(QueuedCandidate::priority).reversed());
      result.discovered = candidates.size();

      int limit = Math.min(enqueueLimitPerFeed, candidates.size());
      for (int i = 0; i < limit; i++) {
        if (enqueueCandidate(candidates.get(i))) {
          result.enqueued++;
        } else {
          result.skipped++;
        }
      }

      feed.setLastChecked(LocalDateTime.now());
      feed.setConsecutiveFailures(0);
      feed.setLastError(null);
      rssFeedConfigRepository.save(feed);
    } catch (Exception e) {
      log.warn("Failed to parse RSS feed {} ({}): {}", feed.getName(), feed.getFeedUrl(), summarizeException(e));
      handleFeedError(feed, e);
      result.skipped++;
    }
    return result;
  }

  private boolean enqueueCandidate(QueuedCandidate candidate) {
    if (queue.size() >= queueCapacity || !queuedUrls.add(candidate.url)) {
      if (queue.size() >= queueCapacity) {
        totalDropped.incrementAndGet();
      }
      return false;
    }
    boolean offered = queue.offer(candidate);
    if (!offered) {
      queuedUrls.remove(candidate.url);
      return false;
    }
    totalEnqueued.incrementAndGet();
    return true;
  }

  private QueueDrainResult drainQueue(int maxItems) {
    if (!processing.compareAndSet(false, true)) {
      return QueueDrainResult.busy();
    }

    try {
      QueueDrainResult result = new QueueDrainResult();
      for (int i = 0; i < Math.max(1, maxItems); i++) {
        QueuedCandidate candidate = pollReady();
        if (candidate == null) {
          break;
        }
        if (synthesizedArticleRepository.findBySourceUrl(candidate.url) != null) {
          dropCandidate(candidate.url);
          result.dropped++;
          totalDropped.incrementAndGet();
          continue;
        }

        ProcessingResult processingResult = processCandidate(candidate);
        switch (processingResult.status) {
          case SUCCESS -> {
            dropCandidate(candidate.url);
            result.processed++;
            totalProcessed.incrementAndGet();
          }
          case RATE_LIMITED -> {
            requeue(candidate, false, processingResult.retryAfterMs);
            result.requeued++;
            totalRateLimited.incrementAndGet();
          }
          case RETRYABLE_FAILURE -> {
            if (candidate.attempt + 1 >= maxRetries) {
              cooldownUrls.put(candidate.url, Instant.now().plus(failedUrlCooldownMinutes, ChronoUnit.MINUTES));
              dropCandidate(candidate.url);
              result.dropped++;
              totalDropped.incrementAndGet();
            } else {
              requeue(candidate, true, computeBackoff(candidate.attempt + 1));
              result.requeued++;
            }
          }
          case PERMANENT_FAILURE -> {
            cooldownUrls.put(candidate.url, Instant.now().plus(failedUrlCooldownMinutes, ChronoUnit.MINUTES));
            dropCandidate(candidate.url);
            result.dropped++;
            totalDropped.incrementAndGet();
          }
        }
      }
      return result;
    } finally {
      processing.set(false);
    }
  }

  private void executeManualRun(String runId, Instant startedAt, int maxArticles, boolean forceEnqueue) {
    int processed = 0;
    int requeued = 0;
    int dropped = 0;
    int idleCycles = 0;
    Map<String, Object> enqueueResult = Map.of();
    try {
      updateManualRunStatus(updateStatus("stage", "ENQUEUEING", "message", "Queueing RSS candidates"));
      enqueueResult = enqueueEligibleFeeds(forceEnqueue);
      updateManualRunStatus(updateStatus(
          "stage", "PROCESSING",
          "message", "Processing queued candidates",
          "enqueue", enqueueResult,
          "queueSize", queue.size()));

      while (processed < maxArticles) {
        int remaining = maxArticles - processed;
        Map<String, Object> batch = processQueueBatch(Math.min(Math.max(1, processBatchSize), remaining));

        if (asBoolean(batch.get("busy"))) {
          sleepQuietly(300);
          continue;
        }

        int batchProcessed = asInt(batch.get("processed"));
        int batchRequeued = asInt(batch.get("requeued"));
        int batchDropped = asInt(batch.get("dropped"));
        int queueSize = asInt(batch.get("queueSize"));

        processed += batchProcessed;
        requeued += batchRequeued;
        dropped += batchDropped;

        int progressPercent = Math.min(99, (int) Math.round((processed * 100.0) / maxArticles));
        updateManualRunStatus(updateStatus(
            "stage", "PROCESSING",
            "processed", processed,
            "requeued", requeued,
            "dropped", dropped,
            "queueSize", queueSize,
            "progressPercent", progressPercent,
            "message", "Processed " + processed + " / " + maxArticles + " requested articles"));

        if (batchProcessed == 0 && batchRequeued == 0 && batchDropped == 0) {
          if (queueSize == 0) {
            break;
          }
          idleCycles++;
          if (idleCycles >= 10) {
            break;
          }
          sleepQuietly(1000);
        } else {
          idleCycles = 0;
        }
      }

      Instant finishedAt = Instant.now();
      Map<String, Object> finalStatus = updateStatus(
          "status", "COMPLETED",
          "stage", "COMPLETED",
          "finishedAt", finishedAt.toString(),
          "durationSeconds", Duration.between(startedAt, finishedAt).toSeconds(),
          "enqueue", enqueueResult,
          "processed", processed,
          "requeued", requeued,
          "dropped", dropped,
          "queueSize", queue.size(),
          "progressPercent", 100,
          "message", "Manual monitoring run completed");
      updateManualRunStatus(finalStatus);
      pushHistory(finalStatus, startedAt);
    } catch (Exception e) {
      Instant finishedAt = Instant.now();
      Map<String, Object> failedStatus = updateStatus(
          "status", "FAILED",
          "stage", "FAILED",
          "finishedAt", finishedAt.toString(),
          "durationSeconds", Duration.between(startedAt, finishedAt).toSeconds(),
          "enqueue", enqueueResult,
          "processed", processed,
          "requeued", requeued,
          "dropped", dropped,
          "queueSize", queue.size(),
          "error", summarizeException(e),
          "message", "Manual monitoring run failed");
      updateManualRunStatus(failedStatus);
      pushHistory(failedStatus, startedAt);
      log.error("Manual RSS monitoring run failed: {}", runId, e);
    } finally {
      manualRunInProgress.set(false);
    }
  }

  private Map<String, Object> updateStatus(Object... kvPairs) {
    Map<String, Object> updated = new HashMap<>(manualRunStatus);
    for (int i = 0; i < kvPairs.length - 1; i += 2) {
      updated.put(String.valueOf(kvPairs[i]), kvPairs[i + 1]);
    }
    return updated;
  }

  private void updateManualRunStatus(Map<String, Object> newStatus) {
    newStatus.put("updatedAt", Instant.now().toString());
    manualRunStatus = Map.copyOf(newStatus);
  }

  private void pushHistory(Map<String, Object> status, Instant startedAt) {
    manualRunHistory.addFirst(new ManualRunSnapshot(startedAt, Map.copyOf(status)));
    while (manualRunHistory.size() > 25) {
      manualRunHistory.removeLast();
    }
  }

  private int asInt(Object value) {
    if (value instanceof Number n) {
      return n.intValue();
    }
    if (value instanceof String s) {
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException ignored) {
        return 0;
      }
    }
    return 0;
  }

  private boolean asBoolean(Object value) {
    if (value instanceof Boolean b) {
      return b;
    }
    if (value instanceof String s) {
      return Boolean.parseBoolean(s);
    }
    return false;
  }

  private void sleepQuietly(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private ScrapePreparation prepareScrapedArticle(QueuedCandidate candidate) {
    ArticleScrapingService.ScrapedArticle scraped = articleScrapingService.scrapeArticle(candidate.url);
    boolean usedSummaryFallback = false;
    if (scraped == null) {
      scraped = new ArticleScrapingService.ScrapedArticle();
    }

    scraped.setSourceUrl(defaultIfBlank(scraped.getSourceUrl(), candidate.url));
    scraped.setTitle(defaultIfBlank(scraped.getTitle(), candidate.title));
    scraped.setAuthor(defaultIfBlank(scraped.getAuthor(), "Unknown"));

    String description = normalizeWhitespace(defaultIfBlank(scraped.getDescription(), ""));
    if (isBlank(description)) {
      description = sanitizeText(candidate.description);
      scraped.setDescription(description);
    }

    String content = normalizeWhitespace(defaultIfBlank(scraped.getContent(), ""));
    if (content.length() < Math.max(40, minContentLength) && !isBlank(description)) {
      String title = defaultIfBlank(scraped.getTitle(), "Untitled");
      content = normalizeWhitespace(title + ". " + description);
      usedSummaryFallback = true;
    }
    scraped.setContent(content);
    return new ScrapePreparation(scraped, usedSummaryFallback);
  }

  private Claim buildFallbackClaim(ArticleScrapingService.ScrapedArticle scraped, QueuedCandidate candidate) {
    String text = normalizeWhitespace(defaultIfBlank(scraped.getDescription(), sanitizeText(candidate.description)));
    if (isBlank(text)) {
      text = normalizeWhitespace(defaultIfBlank(scraped.getTitle(), candidate.title));
    }
    if (isBlank(text)) {
      return null;
    }
    if (text.length() > 280) {
      text = text.substring(0, 280);
    }
    Claim claim = new Claim();
    claim.setText(text);
    claim.setType("FACTUAL");
    claim.setImportance("5");
    claim.setCreatedAt(LocalDateTime.now());
    return claim;
  }

  private ProcessingResult processCandidate(QueuedCandidate candidate) {
    try {
      if (rateLimiter.getAvailableTokens(GROQ_PROVIDER) < 2 || rateLimiter.getAvailableTokens(OPENAI_PROVIDER) < 2) {
        return ProcessingResult.rateLimited(Math.max(
            rateLimiter.getWaitTimeMillis(GROQ_PROVIDER), rateLimiter.getWaitTimeMillis(OPENAI_PROVIDER)));
      }

      ScrapePreparation scrapePreparation = prepareScrapedArticle(candidate);
      ArticleScrapingService.ScrapedArticle scraped = scrapePreparation.article();
      if (isBlank(scraped.getContent())) {
        return ProcessingResult.permanentFailure();
      }
      int minLength = scrapePreparation.usedSummaryFallback()
          ? Math.max(40, minRssSummaryLength)
          : Math.max(40, minContentLength);
      if (scraped.getContent().length() < minLength) {
        return ProcessingResult.permanentFailure();
      }

      if (!rateLimiter.allowRequest(GROQ_PROVIDER)) {
        return ProcessingResult.rateLimited(rateLimiter.getWaitTimeMillis(GROQ_PROVIDER));
      }
      int maxClaims = Math.max(1, maxClaimsPerArticle);
      List<Claim> extractedClaims = claimExtractionService.extractClaims(scraped.getContent());
      List<Claim> claims = prioritizeClaims(extractedClaims, maxClaims);
      if (claims.isEmpty()) {
        Claim fallbackClaim = buildFallbackClaim(scraped, candidate);
        if (fallbackClaim == null) {
          return ProcessingResult.permanentFailure();
        }
        claims = List.of(fallbackClaim);
      }

      int openAiBudget = Math.max(1, rateLimiter.getAvailableTokens(OPENAI_PROVIDER) - 1);
      List<Claim> selectedClaims = claims.stream().limit(Math.min(openAiBudget, maxClaims)).toList();
      List<Verification> verifications = new ArrayList<>();
      for (Claim claim : selectedClaims) {
        if (!rateLimiter.allowRequest(OPENAI_PROVIDER)) {
          return ProcessingResult.rateLimited(rateLimiter.getWaitTimeMillis(OPENAI_PROVIDER));
        }
        Verification verification = verificationService.verifyClaim(claim.getText());
        verification.setClaim(claim);
        if (verification.getEvidence() != null) {
          for (Evidence evidence : verification.getEvidence()) {
            evidence.setVerification(verification);
          }
        }
        claim.setVerification(verification);
        verifications.add(verification);
      }

      if (!rateLimiter.allowRequest(OPENAI_PROVIDER)) {
        return ProcessingResult.rateLimited(rateLimiter.getWaitTimeMillis(OPENAI_PROVIDER));
      }
      Synthesis synthesis = synthesisService.synthesizeVerifications(
          Article.builder().title(scraped.getTitle()).content(scraped.getContent()).url(candidate.url).build(),
          verifications);

      if (!rateLimiter.allowRequest(GROQ_PROVIDER)) {
        return ProcessingResult.rateLimited(rateLimiter.getWaitTimeMillis(GROQ_PROVIDER));
      }
      Category category = resolveCategory(scraped.getTitle(), scraped.getContent());
      persistSynthesized(candidate, scraped, synthesis, selectedClaims, verifications, category);
      return ProcessingResult.success();
    } catch (Exception e) {
      log.error("Failed processing queued candidate: {}", candidate.url, e);
      return ProcessingResult.retryableFailure();
    }
  }

  @Transactional
  protected void persistSynthesized(
      QueuedCandidate candidate,
      ArticleScrapingService.ScrapedArticle scraped,
      Synthesis synthesis,
      List<Claim> claims,
      List<Verification> verifications,
      Category category) {
    long trueClaims = verifications.stream().filter(this::isTrueLike).count();
    long falseClaims = verifications.stream().filter(this::isFalseLike).count();
    long unverifiable = verifications.stream().filter(v -> "UNVERIFIABLE".equalsIgnoreCase(v.getVerdict())).count();

    SynthesizedArticle article = SynthesizedArticle.builder()
        .title(defaultIfBlank(scraped.getTitle(), candidate.title))
        .originalContent(defaultIfBlank(scraped.getContent(), candidate.description))
        .synthesizedNarrative(defaultIfBlank(synthesis.getSummary(), defaultIfBlank(scraped.getDescription(), candidate.title)))
        .sourceUrl(candidate.url)
        .originalSource(candidate.url)
        .rssFeedSource(candidate.feedName)
        .author(defaultIfBlank(scraped.getAuthor(), "Unknown"))
        .imageUrl(scraped.getImageUrl())
        .publishDate(candidate.pubDate == null ? LocalDateTime.now() : candidate.pubDate)
        .credibilityScore(synthesis.getCredibilityScore() == null ? 50.0 : synthesis.getCredibilityScore())
        .status("VERIFIED")
        .verdict(mapVerdict(synthesis.getOverallVerdict(), trueClaims, falseClaims, unverifiable))
        .category(category)
        .keyFindings(defaultIfBlank(synthesis.getKeyFindings(), "Verification summary generated from extracted claims."))
        .timeline(buildTimelineNarrative(candidate, scraped, claims, verifications))
        .claimsCount(claims.size())
        .verifiedClaimsCount(verifications.size())
        .trueClaims((int) trueClaims)
        .falseClaims((int) falseClaims)
        .unverifiableClaims((int) unverifiable)
        .viewCount(0L)
        .isTrending((synthesis.getCredibilityScore() != null && synthesis.getCredibilityScore() >= 75.0) || trueClaims >= 3)
        .build();

    SynthesizedArticle saved = synthesizedArticleRepository.save(article);
    ragContextService.indexSynthesizedArticle(saved);
    rssFeedConfigRepository.findById(candidate.feedId).ifPresent(feed -> {
      feed.setArticlesProcessed((feed.getArticlesProcessed() == null ? 0L : feed.getArticlesProcessed()) + 1);
      feed.setConsecutiveFailures(0);
      feed.setLastError(null);
      rssFeedConfigRepository.save(feed);
    });
  }

  private String buildTimelineNarrative(
      QueuedCandidate candidate,
      ArticleScrapingService.ScrapedArticle scraped,
      List<Claim> claims,
      List<Verification> verifications) {
    List<String> timelineSignals = new ArrayList<>();
    String title = defaultIfBlank(scraped.getTitle(), candidate.title);
    if (!isBlank(title)) {
      timelineSignals.add("Headline event: " + title);
    }
    if (candidate.pubDate != null) {
      timelineSignals.add("Published at " + candidate.pubDate + " via " + defaultIfBlank(candidate.feedName, "RSS feed"));
    }
    for (int i = 0; i < Math.min(5, claims.size()); i++) {
      Claim claim = claims.get(i);
      if (!isBlank(claim.getText())) {
        timelineSignals.add("Claim " + (i + 1) + ": " + claim.getText());
      }
    }
    for (int i = 0; i < Math.min(5, verifications.size()); i++) {
      Verification verification = verifications.get(i);
      String verdict = defaultIfBlank(verification.getVerdict(), "UNVERIFIABLE");
      String explanation = defaultIfBlank(verification.getExplanation(), "");
      timelineSignals.add("Verification " + (i + 1) + ": " + verdict + ". " + explanation);
    }

    try {
      List<TimelineBuilderService.TimelineEvent> events = timelineBuilderService.extractTimeline(timelineSignals);
      if (!events.isEmpty()) {
        events.sort(Comparator.comparing(event -> event.getDate() == null ? LocalDateTime.MIN : event.getDate()));
        return timelineBuilderService.buildTimelineNarrative(events);
      }
    } catch (Exception e) {
      log.debug("Timeline model extraction failed for {}: {}", candidate.url, summarizeException(e));
    }

    return buildFallbackTimeline(candidate, scraped, verifications);
  }

  private String buildFallbackTimeline(
      QueuedCandidate candidate,
      ArticleScrapingService.ScrapedArticle scraped,
      List<Verification> verifications) {
    LocalDateTime publishedAt = candidate.pubDate == null ? LocalDateTime.now() : candidate.pubDate;
    StringBuilder timeline = new StringBuilder("Timeline of Events:\n\n");
    timeline.append("[").append(publishedAt.toLocalDate()).append("] ")
        .append(defaultIfBlank(scraped.getTitle(), candidate.title))
        .append("\n");

    if (!isBlank(candidate.feedName)) {
      timeline.append("[").append(publishedAt.toLocalDate()).append("] ")
          .append("Detected via ").append(candidate.feedName)
          .append("\n");
    }

    if (!verifications.isEmpty()) {
      for (int i = 0; i < Math.min(3, verifications.size()); i++) {
        Verification verification = verifications.get(i);
        timeline.append("[").append(LocalDateTime.now().toLocalDate()).append("] ")
            .append("Claim check #").append(i + 1).append(": ")
            .append(defaultIfBlank(verification.getVerdict(), "UNVERIFIABLE"))
            .append("\n");
      }
    } else {
      timeline.append("[").append(LocalDateTime.now().toLocalDate()).append("] ")
          .append("Verification context generated from available feed metadata.\n");
    }

    return timeline.toString().trim();
  }

  private Category resolveCategory(String title, String content) {
    CategoryTaggingService.CategoryTaggingResult result = categoryTaggingService
        .categorizeArticle(defaultIfBlank(title, "Untitled"), defaultIfBlank(content, ""));
    String name = result.getPrimaryCategory().getDisplayName();
    return categoryRepository.findByName(name).orElseGet(() -> {
      Category category = new Category();
      category.setName(name);
      category.setColor(result.getPrimaryCategory().getColor());
      return categoryRepository.save(category);
    });
  }

  private List<FeedItem> parseFeed(String feedUrl, int maxItems) throws Exception {
    List<FeedItem> items = new ArrayList<>();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

    DocumentBuilder builder = factory.newDocumentBuilder();
    try (InputStream stream = openFeedStream(feedUrl)) {
      Document doc = builder.parse(stream);
      NodeList itemNodes = doc.getElementsByTagName("item");
      if (itemNodes.getLength() > 0) {
        for (int i = 0; i < Math.min(itemNodes.getLength(), maxItems); i++) {
          Element e = (Element) itemNodes.item(i);
          String link = getElementValue(e, "link");
          if (isBlank(link)) {
            link = getElementValue(e, "guid");
          }
          if (!isBlank(link)) {
            items.add(new FeedItem(
                getElementValue(e, "title"),
                link,
                getElementValue(e, "description"),
                parseDate(getElementValue(e, "pubDate"))));
          }
        }
      } else {
        NodeList entryNodes = doc.getElementsByTagName("entry");
        for (int i = 0; i < Math.min(entryNodes.getLength(), maxItems); i++) {
          Element e = (Element) entryNodes.item(i);
          String link = extractAtomLink(e);
          if (!isBlank(link)) {
            items.add(new FeedItem(
                getElementValue(e, "title"),
                link,
                getElementValue(e, "summary"),
                parseDate(getElementValue(e, "updated"))));
          }
        }
      }
    }
    return items;
  }

  private InputStream openFeedStream(String feedUrl) throws IOException {
    URLConnection connection = bypassSystemProxy
        ? new URL(feedUrl).openConnection(Proxy.NO_PROXY)
        : new URL(feedUrl).openConnection();
    connection.setConnectTimeout(8000);
    connection.setReadTimeout(8000);
    connection.setRequestProperty("User-Agent", "SatornRSS/1.0");
    connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*");

    if (connection instanceof HttpURLConnection http) {
      http.setInstanceFollowRedirects(true);
      int status = http.getResponseCode();
      if (status >= 400) {
        throw new IOException("HTTP " + status + " while fetching feed");
      }
    }

    InputStream input = connection.getInputStream();
    String encoding = connection.getContentEncoding();
    if (encoding != null && encoding.toLowerCase(Locale.ROOT).contains("gzip")) {
      return new GZIPInputStream(input);
    }
    return input;
  }

  private String getElementValue(Element element, String tagName) {
    NodeList nodeList = element.getElementsByTagName(tagName);
    return nodeList.getLength() > 0 ? nodeList.item(0).getTextContent() : null;
  }

  private String extractAtomLink(Element entryElement) {
    NodeList links = entryElement.getElementsByTagName("link");
    for (int i = 0; i < links.getLength(); i++) {
      Node node = links.item(i);
      if (node instanceof Element element) {
        String rel = element.getAttribute("rel");
        String href = element.getAttribute("href");
        if (!isBlank(href) && (isBlank(rel) || "alternate".equalsIgnoreCase(rel))) {
          return href;
        }
      }
    }
    return null;
  }

  private LocalDateTime parseDate(String value) {
    if (isBlank(value)) {
      return LocalDateTime.now();
    }
    try {
      return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
    } catch (Exception ignored) {
      try {
        return ZonedDateTime.parse(value).toLocalDateTime();
      } catch (Exception e) {
        return LocalDateTime.now();
      }
    }
  }

  private String normalizeUrl(String rawUrl) {
    if (isBlank(rawUrl)) {
      return null;
    }
    try {
      URI uri = URI.create(rawUrl.trim());
      String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
      String host = uri.getHost();
      if (host == null) {
        return rawUrl.trim();
      }
      return new URI(scheme, null, host.toLowerCase(Locale.ROOT), uri.getPort(), uri.getPath(), null, null).toString();
    } catch (Exception e) {
      return rawUrl.trim();
    }
  }

  private double scoreItem(FeedItem item, RssFeedConfig feed) {
    long hours = Math.max(0, Duration.between(item.pubDate == null ? LocalDateTime.now() : item.pubDate, LocalDateTime.now()).toHours());
    double recency = hours <= 1 ? 40 : hours <= 6 ? 30 : hours <= 24 ? 20 : hours <= 72 ? 10 : 4;
    String text = (defaultIfBlank(item.title, "") + " " + defaultIfBlank(item.description, "")).toLowerCase(Locale.ROOT);
    double signal = 0;
    for (String token : List.of("breaking", "election", "war", "attack", "court", "market", "inflation", "ai", "cyber")) {
      if (text.contains(token)) {
        signal += 3;
      }
    }
    double feedFreq = Math.max(3, 15 - (safeUpdateFrequency(feed) / 10.0));
    double regionBoost = (isIndianSource(item.link) || isIndianSource(feed.getFeedUrl())) ? 8 : 0;
    double categoryBoost = isHighEngagementCategory(feed.getCategory()) ? 5 : 0;
    return recency + Math.min(signal, 30) + feedFreq + regionBoost + categoryBoost;
  }

  private boolean shouldCheckFeed(RssFeedConfig feed) {
    if (feed.getLastChecked() == null) {
      return true;
    }
    return LocalDateTime.now().isAfter(feed.getLastChecked().plusMinutes(safeUpdateFrequency(feed)));
  }

  private void handleFeedError(RssFeedConfig feed, Exception e) {
    int failures = (feed.getConsecutiveFailures() == null ? 0 : feed.getConsecutiveFailures()) + 1;
    feed.setConsecutiveFailures(failures);
    feed.setLastError(summarizeException(e));
    if (failures > 5) {
      feed.setEnabled(false);
    }
    rssFeedConfigRepository.save(feed);
  }

  private QueuedCandidate pollReady() {
    QueuedCandidate candidate = queue.peek();
    if (candidate == null || candidate.nextAttemptAt.isAfter(Instant.now())) {
      return null;
    }
    return queue.poll();
  }

  private void requeue(QueuedCandidate candidate, boolean incrementAttempt, long delayMs) {
    int attempt = incrementAttempt ? candidate.attempt + 1 : candidate.attempt;
    queue.offer(candidate.withRetry(attempt, Instant.now().plusMillis(Math.max(1000, delayMs))));
  }

  private long computeBackoff(int attempt) {
    long safe = Math.min(6, Math.max(1, attempt));
    return retryBaseDelayMs * (1L << (safe - 1));
  }

  private boolean isInCooldown(String url) {
    Instant until = cooldownUrls.get(url);
    if (until == null) {
      return false;
    }
    if (until.isBefore(Instant.now())) {
      cooldownUrls.remove(url);
      return false;
    }
    return true;
  }

  private void dropCandidate(String url) {
    queuedUrls.remove(url);
  }

  private boolean isTrueLike(Verification v) {
    String verdict = v.getVerdict() == null ? "" : v.getVerdict().toUpperCase(Locale.ROOT);
    return verdict.equals("VERIFIED") || verdict.equals("PARTIALLY_VERIFIED")
        || verdict.equals("MOSTLY_TRUE") || verdict.equals("TRUE");
  }

  private int safeUpdateFrequency(RssFeedConfig feed) {
    Integer minutes = feed.getUpdateFrequencyMinutes();
    return (minutes == null || minutes <= 0) ? 60 : minutes;
  }

  private boolean isIndianSource(String url) {
    if (isBlank(url)) {
      return false;
    }
    try {
      String host = URI.create(url.trim()).getHost();
      if (host == null) {
        return false;
      }
      String lowerHost = host.toLowerCase(Locale.ROOT);
      return lowerHost.endsWith(".in")
          || lowerHost.contains("indiatimes")
          || lowerHost.contains("ndtv")
          || lowerHost.contains("timesofindia")
          || lowerHost.contains("thehindu")
          || lowerHost.contains("hindustantimes")
          || lowerHost.contains("indianexpress");
    } catch (Exception ignored) {
      return false;
    }
  }

  private boolean isHighEngagementCategory(String category) {
    if (category == null) {
      return false;
    }
    String cat = category.toLowerCase(Locale.ROOT);
    return cat.contains("politics") || cat.contains("crime") || cat.contains("election");
  }

  private boolean isFalseLike(Verification v) {
    String verdict = v.getVerdict() == null ? "" : v.getVerdict().toUpperCase(Locale.ROOT);
    return verdict.equals("CONTRADICTED") || verdict.equals("MOSTLY_FALSE") || verdict.equals("FALSE");
  }

  private String mapVerdict(String overallVerdict, long trueClaims, long falseClaims, long unverifiable) {
    String verdict = overallVerdict == null ? "" : overallVerdict.toUpperCase(Locale.ROOT);
    if (verdict.equals("CREDIBLE") || verdict.equals("MOSTLY_CREDIBLE")) {
      return "MOSTLY_TRUE";
    }
    if (verdict.equals("NOT_CREDIBLE") || verdict.equals("MOSTLY_UNRELIABLE")) {
      return "MOSTLY_FALSE";
    }
    if (falseClaims > trueClaims) {
      return "MOSTLY_FALSE";
    }
    if (trueClaims > falseClaims * 2) {
      return "MOSTLY_TRUE";
    }
    return unverifiable > trueClaims + falseClaims ? "UNVERIFIABLE" : "MIXED";
  }

  private String defaultIfBlank(String value, String fallback) {
    return isBlank(value) ? fallback : value;
  }

  private List<Claim> prioritizeClaims(List<Claim> claims, int maxClaims) {
    if (claims == null || claims.isEmpty()) {
      return List.of();
    }
    int safeMax = Math.max(1, maxClaims);
    return claims.stream()
        .sorted((a, b) -> Integer.compare(parseImportance(b.getImportance()), parseImportance(a.getImportance())))
        .limit(safeMax)
        .toList();
  }

  private int parseImportance(String importance) {
    if (isBlank(importance)) {
      return 0;
    }
    try {
      return Integer.parseInt(importance.trim());
    } catch (NumberFormatException e) {
      return switch (importance.trim().toUpperCase(Locale.ROOT)) {
        case "HIGH" -> 9;
        case "MEDIUM" -> 5;
        case "LOW" -> 2;
        default -> 0;
      };
    }
  }

  private String sanitizeText(String value) {
    if (value == null) {
      return null;
    }
    return normalizeWhitespace(value.replaceAll("<[^>]*>", " "));
  }

  private String normalizeWhitespace(String value) {
    if (value == null) {
      return null;
    }
    return value.replaceAll("\\s+", " ").trim();
  }

  private String summarizeException(Exception e) {
    Throwable root = e;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    String type = root.getClass().getSimpleName();
    String message = root.getMessage();
    return isBlank(message) ? type : (type + ": " + message);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private record ScrapePreparation(ArticleScrapingService.ScrapedArticle article, boolean usedSummaryFallback) {
  }

  private record FeedItem(String title, String link, String description, LocalDateTime pubDate) {
  }

  private record QueuedCandidate(
      long sequence,
      Long feedId,
      String feedName,
      String url,
      String title,
      String description,
      LocalDateTime pubDate,
      double priority,
      Instant nextAttemptAt,
      int attempt) implements Comparable<QueuedCandidate> {

    @Override
    public int compareTo(QueuedCandidate other) {
      int byReady = this.nextAttemptAt.compareTo(other.nextAttemptAt);
      if (byReady != 0) {
        return byReady;
      }
      int byPriority = Double.compare(other.priority, this.priority);
      if (byPriority != 0) {
        return byPriority;
      }
      return Long.compare(this.sequence, other.sequence);
    }

    QueuedCandidate withRetry(int newAttempt, Instant newNextAttempt) {
      return new QueuedCandidate(sequence, feedId, feedName, url, title, description, pubDate, priority, newNextAttempt,
          newAttempt);
    }
  }

  private static final class FeedEnqueueResult {
    int discovered;
    int enqueued;
    int duplicates;
    int skipped;
    boolean skippedByFrequency;

    static FeedEnqueueResult skippedByFrequency() {
      FeedEnqueueResult result = new FeedEnqueueResult();
      result.skippedByFrequency = true;
      return result;
    }
  }

  private static final class QueueDrainResult {
    boolean busy;
    int processed;
    int requeued;
    int dropped;

    static QueueDrainResult busy() {
      QueueDrainResult result = new QueueDrainResult();
      result.busy = true;
      return result;
    }
  }

  private record ManualRunSnapshot(Instant startedAt, Map<String, Object> status) {
  }

  private enum ProcessStatus {
    SUCCESS, RATE_LIMITED, RETRYABLE_FAILURE, PERMANENT_FAILURE
  }

  private record ProcessingResult(ProcessStatus status, long retryAfterMs) {
    static ProcessingResult success() {
      return new ProcessingResult(ProcessStatus.SUCCESS, 0);
    }

    static ProcessingResult rateLimited(long retryAfterMs) {
      return new ProcessingResult(ProcessStatus.RATE_LIMITED, retryAfterMs);
    }

    static ProcessingResult retryableFailure() {
      return new ProcessingResult(ProcessStatus.RETRYABLE_FAILURE, 0);
    }

    static ProcessingResult permanentFailure() {
      return new ProcessingResult(ProcessStatus.PERMANENT_FAILURE, 0);
    }
  }
}
