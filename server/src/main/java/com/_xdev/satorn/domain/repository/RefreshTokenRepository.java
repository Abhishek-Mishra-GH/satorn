package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.RefreshToken;
import com._xdev.satorn.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /**
   * Find refresh token by token string
   */
  Optional<RefreshToken> findByToken(String token);

  /**
   * Find all tokens for a user
   */
  List<RefreshToken> findByUser(User user);

  /**
   * Find valid (non-expired, non-revoked) token
   */
  @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false AND rt.expiresAt > :now")
  Optional<RefreshToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

  /**
   * Delete all tokens for a user
   */
  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
  void deleteByUser(@Param("user") User user);

  /**
   * Delete expired tokens
   */
  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
  void deleteExpiredTokens(@Param("now") LocalDateTime now);

  /**
   * Revoke all tokens for a user
   */
  @Modifying
  @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
  void revokeAllUserTokens(@Param("user") User user);

  /**
   * Count valid tokens for user
   */
  @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
  long countValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
}
