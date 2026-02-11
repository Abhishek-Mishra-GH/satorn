package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Synthesized articles from RSS feeds
 * Contains verified, synthesized content with timeline and key insights
 */
@Entity
@Table(name = "synthesized_articles", indexes = {
    @Index(name = "idx_synthesized_status", columnList = "status"),
    @Index(name = "idx_synthesized_category", columnList = "category_id"),
    @Index(name = "idx_synthesized_created", columnList = "created_at DESC"),
    @Index(name = "idx_synthesized_source_url", columnList = "source_url")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SynthesizedArticle {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String originalContent;

  @Column(columnDefinition = "TEXT")
  private String synthesizedNarrative;

  @Column(name = "source_url", length = 500)
  private String sourceUrl;

  @Column(name = "original_source")
  private String originalSource;

  @Column(name = "rss_feed_source")
  private String rssFeedSource;

  @Column(name = "author")
  private String author;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(name = "publish_date")
  private LocalDateTime publishDate;

  @Column(name = "credibility_score")
  private Double credibilityScore;

  @Column(name = "status")
  private String status; // PENDING, VERIFIED, REJECTED, ARCHIVED

  @Column(name = "verdict")
  private String verdict; // TRUE, FALSE, PARTIALLY_TRUE, MIXED, UNVERIFIABLE

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private Category category;

  @Column(columnDefinition = "TEXT")
  private String keyFindings;

  @Column(columnDefinition = "TEXT")
  private String timeline;

  @Column(name = "claims_count")
  private Integer claimsCount;

  @Column(name = "verified_claims_count")
  private Integer verifiedClaimsCount;

  @Column(name = "true_claims")
  private Integer trueClaims;

  @Column(name = "false_claims")
  private Integer falseClaims;

  @Column(name = "unverifiable_claims")
  private Integer unverifiableClaims;

  @Column(name = "view_count")
  private Long viewCount = 0L;

  @Column(name = "is_trending")
  private Boolean isTrending = false;

  @OneToMany(mappedBy = "synthesizedArticle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SynthesizedArticleClaim> claims;

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
