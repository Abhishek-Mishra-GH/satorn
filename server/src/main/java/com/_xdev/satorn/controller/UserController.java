package com._xdev.satorn.controller;

import com._xdev.satorn.dto.auth.MessageResponse;
import com._xdev.satorn.dto.auth.UserInfoResponse;
import com._xdev.satorn.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

  private final UserService userService;

  /**
   * Get current user profile
   * GET /api/users/profile
   */
  @GetMapping("/profile")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<UserInfoResponse> getUserProfile(Authentication authentication) {
    try {
      String username = authentication.getName();
      UserInfoResponse userInfo = userService.getUserInfo(username);
      return ResponseEntity.ok(userInfo);
    } catch (Exception e) {
      log.error("Failed to get user profile: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Update user profile
   * PUT /api/users/profile
   */
  @PutMapping("/profile")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<?> updateProfile(
      @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName,
      @RequestParam(required = false) String email,
      Authentication authentication) {
    try {
      String username = authentication.getName();
      UserInfoResponse updatedUser = userService.updateUserProfile(
          username, firstName, lastName, email);
      log.info("Profile updated for user: {}", username);
      return ResponseEntity.ok(updatedUser);
    } catch (RuntimeException e) {
      log.error("Profile update failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Update failed: " + e.getMessage()));
    }
  }

  /**
   * Change password
   * PUT /api/users/password
   */
  @PutMapping("/password")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<?> changePassword(
      @RequestParam String oldPassword,
      @RequestParam String newPassword,
      Authentication authentication) {
    try {
      String username = authentication.getName();
      userService.changePassword(username, oldPassword, newPassword);
      log.info("Password changed for user: {}", username);
      return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    } catch (RuntimeException e) {
      log.error("Password change failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Password change failed: " + e.getMessage()));
    }
  }
}
