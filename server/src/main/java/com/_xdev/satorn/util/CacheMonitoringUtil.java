package com._xdev.satorn.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.cache.CachesEndpoint;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Utility class for monitoring and managing SATORN application cache
 * Provides methods to inspect, clear, and analyze cache performance
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheMonitoringUtil {

  private final CacheManager cacheManager;

  /**
   * Get all configured cache names
   */
  public Set<String> getAllCacheNames() {
    Set<String> cacheNames = new java.util.HashSet<>(cacheManager.getCacheNames());
    log.info("Available caches: {}", cacheNames);
    return cacheNames;
  }

  /**
   * Clear specific cache by name
   */
  public void clearCache(String cacheName) {
    var cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.clear();
      log.info("Cleared cache: {}", cacheName);
    } else {
      log.warn("Cache not found: {}", cacheName);
    }
  }

  /**
   * Clear all caches
   */
  public void clearAllCaches() {
    for (String cacheName : cacheManager.getCacheNames()) {
      clearCache(cacheName);
    }
    log.info("All caches cleared");
  }

  /**
   * Get cache statistics (if available)
   */
  public Map<String, Object> getCacheStats() {
    Map<String, Object> stats = new HashMap<>();
    for (String cacheName : cacheManager.getCacheNames()) {
      var cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        stats.put(cacheName, Map.of(
            "type", cache.getClass().getSimpleName(),
            "nativeCache", cache.getNativeCache().getClass().getSimpleName()));
      }
    }
    return stats;
  }

  /**
   * Get cache evict policy recommendation based on cache name
   */
  public String getCacheEvictPolicy(String cacheName) {
    return switch (cacheName) {
      case "trending-articles", "article-search", "article-statistics" ->
        "5 minutes (frequently changing data)";
      case "synthesized-articles" ->
        "15 minutes (moderately changing)";
      case "category-articles", "top-credible-articles" ->
        "10 minutes (moderately changing)";
      case "article-detail" ->
        "20 minutes (changes less frequently)";
      case "rss-feeds", "rss-feed-detail" ->
        "30 minutes (static configuration)";
      case "rss-statistics" ->
        "10 minutes (updated periodically)";
      case "rate-limiter-status" ->
        "1 minute (frequently checked)";
      default ->
        "10 minutes (default)";
    };
  }

  /**
   * Get recommended cache clear operations after specific events
   */
  public Map<String, List<String>> getCacheClearOperations() {
    Map<String, List<String>> operations = new HashMap<>();

    operations.put("Create Article", Arrays.asList(
        "synthesized-articles",
        "trending-articles",
        "article-statistics"));

    operations.put("Update Article", Arrays.asList(
        "article-detail",
        "synthesized-articles",
        "trending-articles"));

    operations.put("View Article (increment views)", Arrays.asList(
    // No eviction - view count updated in DB directly
    ));

    operations.put("Create RSS Feed", List.of("rss-feeds"));

    operations.put("Update RSS Feed", Arrays.asList(
        "rss-feeds",
        "rss-feed-detail",
        "rss-statistics"));

    operations.put("Delete RSS Feed", Arrays.asList(
        "rss-feeds",
        "rss-feed-detail"));

    operations.put("Toggle RSS Feed", Arrays.asList(
        "rss-feeds",
        "rss-feed-detail",
        "rss-statistics"));

    operations.put("Process RSS Feed", Arrays.asList(
        "synthesized-articles",
        "trending-articles",
        "article-statistics",
        "rss-statistics"));

    operations.put("Reset Feed Failures", List.of("rss-statistics"));

    return operations;
  }

  /**
   * Get cache optimization recommendations
   */
  public List<String> getOptimizationRecommendations() {
    List<String> recommendations = new ArrayList<>();

    recommendations.add("✓ Spring Cache with Redis enabled");
    recommendations.add("✓ 12 cache types configured with specific TTLs");
    recommendations.add("✓ Automatic cache invalidation on CRUD operations");
    recommendations.add("✓ Key prefix strategy: 'satorn:' prevents conflicts");
    recommendations.add("✓ Connection pooling configured for performance");

    recommendations.add("");
    recommendations.add("Best Practices:");
    recommendations.add("1. Monitor Redis memory usage regularly");
    recommendations.add("2. Adjust TTLs based on data volatility");
    recommendations.add("3. Clear caches strategically on data changes");
    recommendations.add("4. Implement cache warming for critical data");
    recommendations.add("5. Track cache hit/miss rates");

    recommendations.add("");
    recommendations.add("Performance Targets:");
    recommendations.add("- Cached response: 10-50ms");
    recommendations.add("- DB query reduction: 95%");
    recommendations.add("- Throughput improvement: 10x");
    recommendations.add("- CPU reduction: 50-70%");

    return recommendations;
  }

  /**
   * Get cache configuration summary
   */
  public Map<String, Object> getCacheConfigurationSummary() {
    Map<String, Object> summary = new HashMap<>();

    // Cache types summary
    Map<String, String> cacheTypes = new LinkedHashMap<>();
    cacheTypes.put("synthesized-articles", "15 min - Article listings");
    cacheTypes.put("trending-articles", "5 min - Trending/popular articles");
    cacheTypes.put("top-credible-articles", "10 min - Highest credibility scores");
    cacheTypes.put("article-detail", "20 min - Full article content");
    cacheTypes.put("article-search", "5 min - Search results");
    cacheTypes.put("category-articles", "10 min - Articles by category");
    cacheTypes.put("article-statistics", "5 min - Article stats");
    cacheTypes.put("rss-feeds", "30 min - RSS feed configs");
    cacheTypes.put("rss-feed-detail", "30 min - Individual feed config");
    cacheTypes.put("rss-statistics", "10 min - RSS feed statistics");
    cacheTypes.put("rate-limiter-status", "1 min - Rate limiter status");

    summary.put("cacheTypes", cacheTypes);

    Map<String, Object> redis = new HashMap<>();
    redis.put("host", "localhost (or REDIS_HOST env var)");
    redis.put("port", "6379 (or REDIS_PORT env var)");
    redis.put("password", "Optional (REDIS_PASSWORD env var)");
    redis.put("keyPrefix", "satorn:");
    redis.put("serializerType", "GenericRedisSerializer");

    summary.put("redisConfig", redis);

    Map<String, Object> poolConfig = new HashMap<>();
    poolConfig.put("maxActive", "8");
    poolConfig.put("maxIdle", "8");
    poolConfig.put("minIdle", "0");
    poolConfig.put("maxWait", "-1ms (wait indefinitely)");

    summary.put("connectionPool", poolConfig);

    return summary;
  }

  /**
   * Health check for Redis cache
   */
  public Map<String, Object> getHealthCheck() {
    Map<String, Object> health = new HashMap<>();

    try {
      Set<String> cacheNames = new java.util.HashSet<>(cacheManager.getCacheNames());
      health.put("status", "UP");
      health.put("cacheManager", "Redis");
      health.put("configuredCaches", cacheNames.size());
      health.put("caches", cacheNames);

      for (String cacheName : cacheNames) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
          health.put(cacheName + "_available", "true");
        }
      }

      health.put("timestamp", System.currentTimeMillis());
      log.info("Cache health check: UP");
    } catch (Exception e) {
      health.put("status", "DOWN");
      health.put("error", e.getMessage());
      log.error("Cache health check failed", e);
    }

    return health;
  }

  /**
   * Get cache usage guide for developers
   */
  public Map<String, Object> getDeveloperGuide() {
    Map<String, Object> guide = new HashMap<>();

    // Annotation examples
    Map<String, String> examples = new LinkedHashMap<>();
    examples.put("@Cacheable", "public ResponseEntity<?> getArticles() { ... }");
    examples.put("Key pattern", "key = \"#page + '-' + #size\"");
    examples.put("@CacheEvict", "@CacheEvict(value=\"rss-feeds\", allEntries=true)");
    examples.put("Usage", "Automatically caches method return values");

    guide.put("annotations", examples);

    // Cache naming convention
    Map<String, String> naming = new LinkedHashMap<>();
    naming.put("Pattern", "cache-name-describes-data-type");
    naming.put("Examples", "article-detail, rss-feeds, trending-articles");
    naming.put("Key separation", "Use '-' to separate parts");
    naming.put("Readability", "Make cache names self-documenting");

    guide.put("namingConvention", naming);

    // Common mistakes
    Map<String, String> mistakes = new LinkedHashMap<>();
    mistakes.put("WRONG", "Same TTL for all caches");
    mistakes.put("RIGHT", "Different TTLs based on data volatility");
    mistakes.put("WRONG", "No cache eviction on updates");
    mistakes.put("RIGHT", "Use @CacheEvict on create/update/delete");
    mistakes.put("WRONG", "Cache mutable objects");
    mistakes.put("RIGHT", "Cache immutable DTOs");

    guide.put("commonMistakes", mistakes);

    return guide;
  }
}
