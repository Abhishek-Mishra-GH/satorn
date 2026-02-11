package com._xdev.satorn.controller;

import com._xdev.satorn.domain.entity.RssFeedConfig;
import com._xdev.satorn.domain.repository.RssFeedConfigRepository;
import com._xdev.satorn.service.feed.RssFeedQueryService;
import com._xdev.satorn.service.feed.RssPreVerificationQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing RSS feed configurations
 * Admin only - manage which RSS feeds to monitor
 */
@RestController
@RequestMapping("/api/admin/rss-feeds")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class RssFeedConfigController {

  private final RssFeedConfigRepository rssFeedConfigRepository;
  private final RssPreVerificationQueueService rssPreVerificationQueueService;
  private final RssFeedQueryService rssFeedQueryService;

  /**
   * List all RSS feed configurations
   * Cached with TTL of 30 minutes (feeds don't change often)
   */
  @GetMapping
  public ResponseEntity<?> listRssFeeds() {
    try {
      return ResponseEntity.ok(rssFeedQueryService.listFeeds());
    } catch (Exception e) {
      log.error("Error listing RSS feeds", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get RSS feed by ID
   * Cached with TTL of 30 minutes
   */
  @GetMapping("/{id}")
  public ResponseEntity<?> getRssFeed(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(rssFeedQueryService.getFeed(id));
    } catch (Exception e) {
      log.error("Error fetching RSS feed", e);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Create new RSS feed configuration
   * Evicts RSS feeds cache when new feed is created
   */
  @PostMapping
  @CacheEvict(value = "rss-feeds", allEntries = true)
  public ResponseEntity<?> createRssFeed(@RequestBody Map<String, Object> request) {
    try {
      String name = (String) request.get("name");
      String feedUrl = (String) request.get("feedUrl");
      String description = (String) request.get("description");
      String category = (String) request.get("category");
      Integer updateFrequency = request.get("updateFrequencyMinutes") != null
          ? Integer.parseInt(request.get("updateFrequencyMinutes").toString())
          : 60;

      // Validate inputs
      if (name == null || name.isEmpty() || feedUrl == null || feedUrl.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Name and feedUrl are required"));
      }

      // Check if feed already exists
      if (rssFeedConfigRepository.findByFeedUrl(feedUrl).isPresent()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "RSS feed already configured"));
      }

      RssFeedConfig feed = RssFeedConfig.builder()
          .name(name)
          .feedUrl(feedUrl)
          .description(description)
          .category(category != null ? category : "General")
          .updateFrequencyMinutes(updateFrequency)
          .enabled(true)
          .articlesProcessed(0L)
          .consecutiveFailures(0)
          .build();

      RssFeedConfig savedFeed = rssFeedConfigRepository.save(feed);

      log.info("Created new RSS feed: {}", name);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(rssFeedQueryService.getFeed(savedFeed.getId()));
    } catch (Exception e) {
      log.error("Error creating RSS feed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Update RSS feed configuration
   * Evicts RSS feeds cache when feed is updated
   */
  @PutMapping("/{id}")
  @CacheEvict(value = { "rss-feeds", "rss-feed-detail" }, allEntries = true)
  public ResponseEntity<?> updateRssFeed(
      @PathVariable Long id,
      @RequestBody Map<String, Object> request) {
    try {
      RssFeedConfig feed = rssFeedConfigRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("RSS feed not found"));

      if (request.get("name") != null) {
        feed.setName((String) request.get("name"));
      }
      if (request.get("description") != null) {
        feed.setDescription((String) request.get("description"));
      }
      if (request.get("category") != null) {
        feed.setCategory((String) request.get("category"));
      }
      if (request.get("updateFrequencyMinutes") != null) {
        feed.setUpdateFrequencyMinutes(Integer.parseInt(request.get("updateFrequencyMinutes").toString()));
      }
      if (request.get("enabled") != null) {
        feed.setEnabled((Boolean) request.get("enabled"));
      }

      RssFeedConfig updatedFeed = rssFeedConfigRepository.save(feed);

      log.info("Updated RSS feed: {}", feed.getName());
      return ResponseEntity.ok(rssFeedQueryService.getFeed(updatedFeed.getId()));
    } catch (Exception e) {
      log.error("Error updating RSS feed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Delete RSS feed configuration
   * Evicts RSS feeds cache when feed is deleted
   */
  @DeleteMapping("/{id}")
  @CacheEvict(value = { "rss-feeds", "rss-feed-detail" }, allEntries = true)
  public ResponseEntity<?> deleteRssFeed(@PathVariable Long id) {
    try {
      RssFeedConfig feed = rssFeedConfigRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("RSS feed not found"));

      rssFeedConfigRepository.delete(feed);

      log.info("Deleted RSS feed: {}", feed.getName());
      return ResponseEntity.ok(Map.of("message", "RSS feed deleted successfully"));
    } catch (Exception e) {
      log.error("Error deleting RSS feed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Enable/disable RSS feed
   * Evicts RSS feeds cache when toggle state changes
   */
  @PutMapping("/{id}/toggle")
  @CacheEvict(value = { "rss-feeds", "rss-feed-detail", "rss-statistics" }, allEntries = true)
  public ResponseEntity<?> toggleRssFeed(@PathVariable Long id) {
    try {
      RssFeedConfig feed = rssFeedConfigRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("RSS feed not found"));

      feed.setEnabled(!feed.getEnabled());
      RssFeedConfig updated = rssFeedConfigRepository.save(feed);

      log.info("Toggled RSS feed {} status to: {}", feed.getName(), updated.getEnabled());
      return ResponseEntity.ok(rssFeedQueryService.getFeed(updated.getId()));
    } catch (Exception e) {
      log.error("Error toggling RSS feed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Manually trigger feed processing
   * Evicts related caches when feed is processed
   */
  @PostMapping("/{id}/process")
  @CacheEvict(value = { "synthesized-articles", "trending-articles", "article-statistics",
      "rss-statistics" }, allEntries = true)
  public ResponseEntity<?> processFeed(@PathVariable Long id) {
    try {
      RssFeedConfig feed = rssFeedConfigRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("RSS feed not found"));

      Map<String, Object> enqueueResult = rssPreVerificationQueueService.enqueueFeed(id, true);
      Map<String, Object> processResult = rssPreVerificationQueueService.processQueueBatch();

      log.info("Manually triggered processing for feed: {}", feed.getName());
      return ResponseEntity.ok(Map.of(
          "message", "Feed queued for pre-verification",
          "enqueue", enqueueResult,
          "queueProcessing", processResult));
    } catch (Exception e) {
      log.error("Error processing RSS feed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Queue all enabled feeds for pre-verification.
   */
  @PostMapping("/process-all")
  @CacheEvict(value = { "synthesized-articles", "trending-articles", "article-statistics",
      "rss-statistics" }, allEntries = true)
  public ResponseEntity<?> processAllFeeds(
      @RequestParam(defaultValue = "20") int maxArticles,
      @RequestParam(defaultValue = "true") boolean forceEnqueue) {
    try {
      Map<String, Object> run = rssPreVerificationQueueService.startManualMonitoringRun(maxArticles, forceEnqueue);
      boolean accepted = Boolean.TRUE.equals(run.get("accepted"));
      HttpStatus status = accepted ? HttpStatus.ACCEPTED : HttpStatus.CONFLICT;
      return ResponseEntity.status(status).body(run);
    } catch (DataAccessException e) {
      log.warn("RSS tables unavailable while starting manual run: {}", e.getMostSpecificCause().getMessage());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "RSS schema not initialized yet", "details", e.getMostSpecificCause().getMessage()));
    } catch (Exception e) {
      log.error("Error starting manual RSS monitoring run", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get status of latest manual process-all run.
   */
  @GetMapping("/process-all/status")
  public ResponseEntity<?> getManualRunStatus() {
    try {
      return ResponseEntity.ok(rssPreVerificationQueueService.getManualRunStatus());
    } catch (Exception e) {
      log.error("Error fetching manual RSS run status", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get recent manual process-all runs.
   */
  @GetMapping("/process-all/history")
  public ResponseEntity<?> getManualRunHistory(@RequestParam(defaultValue = "10") int limit) {
    try {
      return ResponseEntity.ok(Map.of(
          "runs", rssPreVerificationQueueService.getManualRunHistory(limit)));
    } catch (Exception e) {
      log.error("Error fetching manual RSS run history", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Process currently queued pre-verification items.
   */
  @PostMapping("/process-queue")
  @CacheEvict(value = { "synthesized-articles", "trending-articles", "article-statistics",
      "rss-statistics" }, allEntries = true)
  public ResponseEntity<?> processQueue(@RequestParam(required = false) Integer maxItems) {
    try {
      Map<String, Object> processResult = (maxItems == null || maxItems <= 0)
          ? rssPreVerificationQueueService.processQueueBatch()
          : rssPreVerificationQueueService.processQueueBatch(maxItems);
      return ResponseEntity.ok(Map.of(
          "message", "Queue processing triggered",
          "result", processResult));
    } catch (DataAccessException e) {
      log.warn("RSS tables unavailable while processing queue: {}", e.getMostSpecificCause().getMessage());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "RSS schema not initialized yet", "details", e.getMostSpecificCause().getMessage()));
    } catch (Exception e) {
      log.error("Error processing RSS pre-verification queue", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get current queue status for RSS pre-verification.
   */
  @GetMapping("/queue-status")
  public ResponseEntity<?> getQueueStatus() {
    try {
      return ResponseEntity.ok(rssPreVerificationQueueService.getQueueStatus());
    } catch (DataAccessException e) {
      log.warn("RSS tables unavailable while fetching queue status: {}", e.getMostSpecificCause().getMessage());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "RSS schema not initialized yet", "details", e.getMostSpecificCause().getMessage()));
    } catch (Exception e) {
      log.error("Error fetching RSS queue status", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Probe RSS feed connectivity and parsing without enqueueing.
   */
  @GetMapping("/{id}/probe")
  public ResponseEntity<?> probeFeed(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(rssPreVerificationQueueService.probeFeed(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", e.getMessage()));
    } catch (DataAccessException e) {
      log.warn("RSS tables unavailable while probing feed: {}", e.getMostSpecificCause().getMessage());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "RSS schema not initialized yet", "details", e.getMostSpecificCause().getMessage()));
    } catch (Exception e) {
      log.error("Error probing RSS feed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Reset consecutive failures for a feed
   * Evicts RSS statistics cache when failures are reset
   */
  @PostMapping("/{id}/reset-failures")
  @CacheEvict(value = "rss-statistics", allEntries = true)
  public ResponseEntity<?> resetFailures(@PathVariable Long id) {
    try {
      RssFeedConfig feed = rssFeedConfigRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("RSS feed not found"));

      feed.setConsecutiveFailures(0);
      feed.setLastError(null);
      RssFeedConfig updated = rssFeedConfigRepository.save(feed);

      log.info("Reset failures for feed: {}", feed.getName());
      return ResponseEntity.ok(rssFeedQueryService.getFeed(updated.getId()));
    } catch (Exception e) {
      log.error("Error resetting feed failures", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get feed statistics
   * Cached with TTL of 10 minutes
   */
  @GetMapping("/statistics")
  public ResponseEntity<?> getStatistics() {
    try {
      return ResponseEntity.ok(rssFeedQueryService.getStatistics());
    } catch (Exception e) {
      log.error("Error fetching feed statistics", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

}
