package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Builder.Default
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Builder.Default
  @Column(nullable = false)
  private boolean revoked = false;

  /**
   * Check if token is expired
   */
  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }

  /**
   * Check if token is valid (not expired and not revoked)
   */
  public boolean isValid() {
    return !isExpired() && !revoked;
  }

  /**
   * Revoke the token
   */
  public void revoke() {
    this.revoked = true;
  }
}
