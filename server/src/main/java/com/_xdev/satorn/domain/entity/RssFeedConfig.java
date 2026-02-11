package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RSS Feed configurations for monitoring
 */
@Entity
@Table(name = "rss_feed_configs", indexes = {
    @Index(name = "idx_rss_enabled", columnList = "enabled"),
    @Index(name = "idx_rss_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RssFeedConfig {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, length = 500, unique = true)
  private String feedUrl;

  @Column(length = 1000)
  private String description;

  @Column(nullable = false)
  private String category; // Politics, Health, Technology, etc.

  @Column(name = "update_frequency_minutes", columnDefinition = "DEFAULT 60")
  private Integer updateFrequencyMinutes = 60; // Default: check every hour

  @Column(name = "last_checked")
  private LocalDateTime lastChecked;

  @Column(name = "enabled")
  private Boolean enabled = true;

  @Column(name = "articles_processed")
  private Long articlesProcessed = 0L;

  @Column(name = "last_error")
  private String lastError;

  @Column(name = "consecutive_failures")
  private Integer consecutiveFailures = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
