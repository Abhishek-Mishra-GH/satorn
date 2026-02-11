package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Find user by username
   */
  Optional<User> findByUsername(String username);

  /**
   * Find user by email
   */
  Optional<User> findByEmail(String email);

  /**
   * Check if username exists
   */
  boolean existsByUsername(String username);

  /**
   * Check if email exists
   */
  boolean existsByEmail(String email);

  /**
   * Find users by role name
   */
  @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
  List<User> findByRoleName(@Param("roleName") String roleName);

  /**
   * Find locked accounts
   */
  @Query("SELECT u FROM User u WHERE u.accountNonLocked = false")
  List<User> findLockedAccounts();

  /**
   * Find users created after specific date
   */
  @Query("SELECT u FROM User u WHERE u.createdAt > :date")
  List<User> findUsersCreatedAfter(@Param("date") LocalDateTime date);

  /**
   * Find users with failed login attempts exceeding threshold
   */
  @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold")
  List<User> findUsersWithFailedAttempts(@Param("threshold") int threshold);

  /**
   * Count users by role
   */
  @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
  long countByRoleName(@Param("roleName") String roleName);

  /**
   * Find enabled users
   */
  List<User> findByEnabled(boolean enabled);
}
