package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "verifications", indexes = {
    @Index(name = "idx_verification_claim", columnList = "claim_id"),
    @Index(name = "idx_verification_verdict", columnList = "verdict")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Verification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "claim_id", nullable = false)
  private Claim claim;

  @Column(nullable = false, length = 50)
  private String verdict;

  @Column(nullable = false)
  private Double confidence;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String explanation;

  @Column(columnDefinition = "TEXT")
  private String methodology;

  @OneToMany(mappedBy = "verification", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Evidence> evidence = new ArrayList<>();

  @Column(name = "verified_at", nullable = false)
  @Builder.Default
  private LocalDateTime verifiedAt = LocalDateTime.now();

  @Column(name = "created_at", updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
}
