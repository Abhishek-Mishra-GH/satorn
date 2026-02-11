package com._xdev.satorn.service;

import com._xdev.satorn.domain.entity.Role;
import com._xdev.satorn.domain.entity.User;
import com._xdev.satorn.domain.repository.RoleRepository;
import com._xdev.satorn.domain.repository.UserRepository;
import com._xdev.satorn.dto.auth.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Get user by ID
   */
  @Transactional(readOnly = true)
  public User getUserById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
  }

  /**
   * Get user by username
   */
  @Transactional(readOnly = true)
  public User getUserByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
  }

  /**
   * Get user info response
   */
  @Transactional(readOnly = true)
  public UserInfoResponse getUserInfo(String username) {
    User user = getUserByUsername(username);
    return mapToUserInfoResponse(user);
  }

  /**
   * Get all users
   */
  @Transactional(readOnly = true)
  public List<UserInfoResponse> getAllUsers() {
    return userRepository.findAll().stream()
        .map(this::mapToUserInfoResponse)
        .collect(Collectors.toList());
  }

  /**
   * Update user profile
   */
  @Transactional
  public UserInfoResponse updateUserProfile(String username, String firstName, String lastName, String email) {
    User user = getUserByUsername(username);

    if (firstName != null)
      user.setFirstName(firstName);
    if (lastName != null)
      user.setLastName(lastName);
    if (email != null && !email.equals(user.getEmail())) {
      if (userRepository.existsByEmail(email)) {
        throw new RuntimeException("Email already in use");
      }
      user.setEmail(email);
    }

    user = userRepository.save(user);
    log.info("User profile updated: {}", username);

    return mapToUserInfoResponse(user);
  }

  /**
   * Change user password
   */
  @Transactional
  public void changePassword(String username, String oldPassword, String newPassword) {
    User user = getUserByUsername(username);

    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
      throw new RuntimeException("Old password is incorrect");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    log.info("Password changed for user: {}", username);
  }

  /**
   * Assign role to user
   */
  @Transactional
  public void assignRole(Long userId, String roleName) {
    User user = getUserById(userId);
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

    user.addRole(role);
    userRepository.save(user);
    log.info("Role {} assigned to user: {}", roleName, user.getUsername());
  }

  /**
   * Remove role from user
   */
  @Transactional
  public void removeRole(Long userId, String roleName) {
    User user = getUserById(userId);
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

    user.removeRole(role);
    userRepository.save(user);
    log.info("Role {} removed from user: {}", roleName, user.getUsername());
  }

  /**
   * Lock user account
   */
  @Transactional
  public void lockAccount(Long userId) {
    User user = getUserById(userId);
    user.lock();
    userRepository.save(user);
    log.info("Account locked: {}", user.getUsername());
  }

  /**
   * Unlock user account
   */
  @Transactional
  public void unlockAccount(Long userId) {
    User user = getUserById(userId);
    user.unlock();
    userRepository.save(user);
    log.info("Account unlocked: {}", user.getUsername());
  }

  /**
   * Handle failed login attempt
   */
  @Transactional
  public void handleFailedLogin(String username, int maxAttempts) {
    userRepository.findByUsername(username).ifPresent(user -> {
      user.incrementFailedAttempts();

      if (user.getFailedLoginAttempts() >= maxAttempts) {
        user.lock();
        log.warn("Account locked due to too many failed attempts: {}", username);
      }

      userRepository.save(user);
    });
  }

  /**
   * Handle successful login
   */
  @Transactional
  public void handleSuccessfulLogin(String username) {
    userRepository.findByUsername(username).ifPresent(user -> {
      user.resetFailedAttempts();
      user.setLastLoginAt(LocalDateTime.now());
      userRepository.save(user);
      log.info("Successful login: {}", username);
    });
  }

  private UserInfoResponse mapToUserInfoResponse(User user) {
    List<String> roles = user.getRoles().stream()
        .map(Role::getName)
        .collect(Collectors.toList());

    return UserInfoResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .roles(roles)
        .createdAt(user.getCreatedAt())
        .lastLoginAt(user.getLastLoginAt())
        .build();
  }
}
