package com._xdev.satorn.controller;

import com._xdev.satorn.dto.auth.MessageResponse;
import com._xdev.satorn.dto.auth.UserInfoResponse;
import com._xdev.satorn.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminController {

  private final UserService userService;

  /**
   * Get all users
   * GET /api/admin/users
   */
  @GetMapping("/users")
  @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
  public ResponseEntity<List<UserInfoResponse>> getAllUsers() {
    try {
      List<UserInfoResponse> users = userService.getAllUsers();
      return ResponseEntity.ok(users);
    } catch (Exception e) {
      log.error("Failed to get users: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Get user by ID
   * GET /api/admin/users/{id}
   */
  @GetMapping("/users/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
  public ResponseEntity<UserInfoResponse> getUserById(@PathVariable Long id) {
    try {
      UserInfoResponse user = userService.getUserInfo(
          userService.getUserById(id).getUsername());
      return ResponseEntity.ok(user);
    } catch (Exception e) {
      log.error("Failed to get user: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Assign role to user
   * PUT /api/admin/users/{id}/roles/add
   */
  @PutMapping("/users/{id}/roles/add")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> assignRole(
      @PathVariable Long id,
      @RequestParam String roleName) {
    try {
      userService.assignRole(id, roleName);
      log.info("Role {} assigned to user ID: {}", roleName, id);
      return ResponseEntity.ok(new MessageResponse("Role assigned successfully"));
    } catch (RuntimeException e) {
      log.error("Failed to assign role: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Failed to assign role: " + e.getMessage()));
    }
  }

  /**
   * Remove role from user
   * PUT /api/admin/users/{id}/roles/remove
   */
  @PutMapping("/users/{id}/roles/remove")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> removeRole(
      @PathVariable Long id,
      @RequestParam String roleName) {
    try {
      userService.removeRole(id, roleName);
      log.info("Role {} removed from user ID: {}", roleName, id);
      return ResponseEntity.ok(new MessageResponse("Role removed successfully"));
    } catch (RuntimeException e) {
      log.error("Failed to remove role: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Failed to remove role: " + e.getMessage()));
    }
  }

  /**
   * Lock user account
   * PUT /api/admin/users/{id}/lock
   */
  @PutMapping("/users/{id}/lock")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> lockAccount(@PathVariable Long id) {
    try {
      userService.lockAccount(id);
      log.info("Account locked for user ID: {}", id);
      return ResponseEntity.ok(new MessageResponse("Account locked successfully"));
    } catch (RuntimeException e) {
      log.error("Failed to lock account: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Failed to lock account: " + e.getMessage()));
    }
  }

  /**
   * Unlock user account
   * PUT /api/admin/users/{id}/unlock
   */
  @PutMapping("/users/{id}/unlock")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> unlockAccount(@PathVariable Long id) {
    try {
      userService.unlockAccount(id);
      log.info("Account unlocked for user ID: {}", id);
      return ResponseEntity.ok(new MessageResponse("Account unlocked successfully"));
    } catch (RuntimeException e) {
      log.error("Failed to unlock account: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Failed to unlock account: " + e.getMessage()));
    }
  }
}
