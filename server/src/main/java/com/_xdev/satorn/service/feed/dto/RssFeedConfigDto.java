package com._xdev.satorn.service.feed.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RssFeedConfigDto {
  private final Long id;
  private final String name;
  private final String feedUrl;
  private final String description;
  private final String category;
  private final Integer updateFrequencyMinutes;
  private final String lastChecked;
  private final Boolean enabled;
  private final Long articlesProcessed;
  private final String lastError;
  private final Integer consecutiveFailures;
  private final String createdAt;
  private final String updatedAt;

  @JsonCreator
  public RssFeedConfigDto(
      @JsonProperty("id") Long id,
      @JsonProperty("name") String name,
      @JsonProperty("feedUrl") String feedUrl,
      @JsonProperty("description") String description,
      @JsonProperty("category") String category,
      @JsonProperty("updateFrequencyMinutes") Integer updateFrequencyMinutes,
      @JsonProperty("lastChecked") String lastChecked,
      @JsonProperty("enabled") Boolean enabled,
      @JsonProperty("articlesProcessed") Long articlesProcessed,
      @JsonProperty("lastError") String lastError,
      @JsonProperty("consecutiveFailures") Integer consecutiveFailures,
      @JsonProperty("createdAt") String createdAt,
      @JsonProperty("updatedAt") String updatedAt) {
    this.id = id;
    this.name = name;
    this.feedUrl = feedUrl;
    this.description = description;
    this.category = category;
    this.updateFrequencyMinutes = updateFrequencyMinutes;
    this.lastChecked = lastChecked;
    this.enabled = enabled;
    this.articlesProcessed = articlesProcessed;
    this.lastError = lastError;
    this.consecutiveFailures = consecutiveFailures;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getFeedUrl() {
    return feedUrl;
  }

  public String getDescription() {
    return description;
  }

  public String getCategory() {
    return category;
  }

  public Integer getUpdateFrequencyMinutes() {
    return updateFrequencyMinutes;
  }

  public String getLastChecked() {
    return lastChecked;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public Long getArticlesProcessed() {
    return articlesProcessed;
  }

  public String getLastError() {
    return lastError;
  }

  public Integer getConsecutiveFailures() {
    return consecutiveFailures;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }
}
