package com._xdev.satorn.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter service for managing API request quotas
 * Implements token bucket algorithm for flexible rate limiting
 */
@Slf4j
@Service
public class RateLimiter {

  private static final class TokenBucket {
    private double tokens;
    private long lastRefillTime;
    private final double maxTokens;
    private final double refillRate; // tokens per minute

    TokenBucket(double maxTokens, double refillRate) {
      this.maxTokens = maxTokens;
      this.refillRate = refillRate;
      this.tokens = maxTokens;
      this.lastRefillTime = System.currentTimeMillis();
    }

    synchronized boolean tryConsume(int tokenCount) {
      refillTokens();
      if (tokens >= tokenCount) {
        tokens -= tokenCount;
        return true;
      }
      return false;
    }

    synchronized int availableTokens() {
      refillTokens();
      return (int) tokens;
    }

    private void refillTokens() {
      long now = System.currentTimeMillis();
      long timePassed = now - lastRefillTime;
      double tokensToAdd = (timePassed / 60000.0) * refillRate; // convert ms to minutes
      tokens = Math.min(maxTokens, tokens + tokensToAdd);
      lastRefillTime = now;
    }
  }

  private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

  /**
   * Create or get a rate limiter for a specific provider
   * Groq: 10 requests per minute
   * OpenAI: 60 requests per minute (conservative)
   */
  public void initializeProvider(String providerName, int requestsPerMinute) {
    buckets.computeIfAbsent(providerName,
        key -> new TokenBucket(requestsPerMinute, requestsPerMinute));
    log.info("Initialized rate limiter for {} with {} requests/min",
        providerName, requestsPerMinute);
  }

  /**
   * Check if a request can be made to the provider
   */
  public boolean allowRequest(String providerName) {
    TokenBucket bucket = buckets.get(providerName);
    if (bucket == null) {
      log.warn("Provider {} not initialized, allowing request", providerName);
      return true;
    }
    return bucket.tryConsume(1);
  }

  /**
   * Get available tokens for a provider
   */
  public int getAvailableTokens(String providerName) {
    TokenBucket bucket = buckets.get(providerName);
    if (bucket == null) {
      return 0;
    }
    return bucket.availableTokens();
  }

  /**
   * Wait until a request is allowed (blocking)
   * Used for sequential processing with rate limiting
   */
  public void waitUntilAllowed(String providerName) throws InterruptedException {
    TokenBucket bucket = buckets.get(providerName);
    if (bucket == null) {
      log.warn("Provider {} not initialized, no rate limiting applied", providerName);
      return;
    }

    while (!bucket.tryConsume(1)) {
      Thread.sleep(1000); // Wait 1 second before retrying
    }
  }

  /**
   * Get remaining time until a request can be made
   */
  public long getWaitTimeMillis(String providerName) {
    TokenBucket bucket = buckets.get(providerName);
    if (bucket == null) {
      return 0;
    }

    if (bucket.availableTokens() > 0) {
      return 0;
    }

    // Calculate wait time based on refill rate
    long timePerToken = (long) (60000.0 / bucket.refillRate);
    return timePerToken;
  }

  /**
   * Reset rate limiter for a provider (admin only)
   */
  public void resetProvider(String providerName) {
    TokenBucket bucket = buckets.get(providerName);
    if (bucket != null) {
      bucket.tokens = bucket.maxTokens;
      bucket.lastRefillTime = System.currentTimeMillis();
      log.info("Reset rate limiter for provider: {}", providerName);
    }
  }

  /**
   * Get current status of all rate limiters
   */
  public Map<String, Map<String, Object>> getStatus() {
    Map<String, Map<String, Object>> status = new HashMap<>();
    for (var entry : buckets.entrySet()) {
      Map<String, Object> providerStatus = new HashMap<>();
      providerStatus.put("availableTokens", entry.getValue().availableTokens());
      providerStatus.put("maxTokens", (int) entry.getValue().maxTokens);
      providerStatus.put("refillRate", entry.getValue().refillRate);
      status.put(entry.getKey(), providerStatus);
    }
    return status;
  }
}
