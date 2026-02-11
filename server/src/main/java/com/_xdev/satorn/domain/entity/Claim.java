package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "claims", indexes = {
    @Index(name = "idx_claim_article", columnList = "article_id"),
    @Index(name = "idx_claim_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false)
  private Article article;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String text;

  @Column(nullable = false, length = 50)
  private String type;

  @Column(length = 50)
  private String importance;

  @Column(name = "extracted_at", nullable = false)
  @Builder.Default
  private LocalDateTime extractedAt = LocalDateTime.now();

  @OneToOne(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
  private Verification verification;

  @Column(name = "created_at", updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
}
