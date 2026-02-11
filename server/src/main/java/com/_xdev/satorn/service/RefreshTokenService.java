package com._xdev.satorn.service;

import com._xdev.satorn.domain.entity.RefreshToken;
import com._xdev.satorn.domain.entity.User;
import com._xdev.satorn.domain.repository.RefreshTokenRepository;
import com._xdev.satorn.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtUtils jwtUtils;

  @Value("${jwt.refresh-expiration}")
  private long refreshTokenDuration;

  /**
   * Create refresh token for user
   */
  @Transactional
  public RefreshToken createRefreshToken(User user) {
    RefreshToken refreshToken = RefreshToken.builder()
        .user(user)
        .token(UUID.randomUUID().toString())
        .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenDuration / 1000))
        .build();

    return refreshTokenRepository.save(refreshToken);
  }

  /**
   * Find valid refresh token
   */
  @Transactional(readOnly = true)
  public Optional<RefreshToken> findByToken(String token) {
    return refreshTokenRepository.findByToken(token);
  }

  /**
   * Verify expiration of token
   */
  @Transactional
  public RefreshToken verifyExpiration(RefreshToken token) {
    if (token.isExpired()) {
      refreshTokenRepository.delete(token);
      throw new RuntimeException("Refresh token expired. Please login again.");
    }
    return token;
  }

  /**
   * Revoke refresh token
   */
  @Transactional
  public void revokeToken(String token) {
    refreshTokenRepository.findByToken(token)
        .ifPresent(refreshToken -> {
          refreshToken.revoke();
          refreshTokenRepository.save(refreshToken);
          log.info("Refresh token revoked for user: {}", refreshToken.getUser().getUsername());
        });
  }

  /**
   * Revoke all tokens for user
   */
  @Transactional
  public void revokeAllUserTokens(User user) {
    refreshTokenRepository.revokeAllUserTokens(user);
    log.info("All refresh tokens revoked for user: {}", user.getUsername());
  }

  /**
   * Delete expired tokens (scheduled cleanup)
   */
  @Transactional
  public void deleteExpiredTokens() {
    refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    log.info("Expired refresh tokens cleaned up");
  }
}
