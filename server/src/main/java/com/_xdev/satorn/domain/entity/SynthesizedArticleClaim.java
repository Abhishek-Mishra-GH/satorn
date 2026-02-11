package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Claims extracted from synthesized articles
 */
@Entity
@Table(name = "synthesized_article_claims", indexes = {
    @Index(name = "idx_syn_claim_article", columnList = "synthesized_article_id"),
    @Index(name = "idx_syn_claim_verdict", columnList = "verdict")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SynthesizedArticleClaim {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "synthesized_article_id", nullable = false)
  private SynthesizedArticle synthesizedArticle;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String claimText;

  @Column(name = "claim_type")
  private String claimType;

  @Column(name = "importance")
  private String importance; // HIGH, MEDIUM, LOW

  @Column(name = "verdict")
  private String verdict; // TRUE, FALSE, PARTIALLY_TRUE, UNVERIFIABLE

  @Column(name = "confidence_score")
  private Double confidenceScore;

  @Column(columnDefinition = "TEXT")
  private String reasoning;

  @Column(columnDefinition = "TEXT")
  private String supportingEvidence;

  @Column(columnDefinition = "TEXT")
  private String contradictingEvidence;
}
