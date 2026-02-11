package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "syntheses", indexes = {
    @Index(name = "idx_synthesis_article", columnList = "article_id"),
    @Index(name = "idx_synthesis_verdict", columnList = "overall_verdict")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Synthesis {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  @Column(nullable = false, length = 50)
  private String overallVerdict;

  @Column(nullable = false)
  private Double credibilityScore;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String summary;

  @Column(columnDefinition = "TEXT")
  private String keyFindings;

  @Column(columnDefinition = "TEXT")
  private String recommendations;

  @Column(name = "verified_claims_count")
  private Integer verifiedClaimsCount;

  @Column(name = "false_claims_count")
  private Integer falseClaimsCount;

  @Column(name = "misleading_claims_count")
  private Integer misleadingClaimsCount;

  @Column(name = "created_at", updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
}
