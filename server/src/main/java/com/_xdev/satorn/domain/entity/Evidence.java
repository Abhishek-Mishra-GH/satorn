package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evidence", indexes = {
    @Index(name = "idx_evidence_verification", columnList = "verification_id"),
    @Index(name = "idx_evidence_relevance", columnList = "relevance_score")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evidence {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "verification_id", nullable = false)
  private Verification verification;

  @Column(nullable = false, length = 2048)
  private String sourceUrl;

  @Column(nullable = false, length = 255)
  private String sourceTitle;

  @Column(length = 100)
  private String sourceDomain;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String snippet;

  @Column(name = "relevance_score")
  private Double relevanceScore;

  @Column(name = "credibility_score")
  private Double credibilityScore;

  @Column(name = "publish_date")
  private LocalDateTime publishDate;

  @Column(name = "retrieved_at", nullable = false)
  @Builder.Default
  private LocalDateTime retrievedAt = LocalDateTime.now();

  @Column(name = "created_at", updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
}
