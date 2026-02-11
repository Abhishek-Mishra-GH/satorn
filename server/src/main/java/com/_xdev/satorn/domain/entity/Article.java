package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "articles", indexes = {
    @Index(name = "idx_article_status", columnList = "status"),
    @Index(name = "idx_article_user", columnList = "submitted_by_id"),
    @Index(name = "idx_article_submitted_at", columnList = "submitted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 2048)
  private String url;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(length = 255)
  private String author;

  @Column(name = "publish_date")
  private LocalDateTime publishDate;

  @Column(nullable = false, length = 50)
  @Builder.Default
  private String status = "SUBMITTED";

  @Column(name = "credibility_score")
  private Double credibilityScore;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submitted_by_id", nullable = false)
  private User submittedBy;

  @Column(name = "submitted_at", nullable = false)
  @Builder.Default
  private LocalDateTime submittedAt = LocalDateTime.now();

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private Category category;

  @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Claim> claims = new ArrayList<>();

  @OneToOne(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
  private Synthesis synthesis;

  @Column(name = "created_at", updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at")
  @Builder.Default
  private LocalDateTime updatedAt = LocalDateTime.now();

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
