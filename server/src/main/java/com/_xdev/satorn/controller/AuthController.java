package com._xdev.satorn.controller;

import com._xdev.satorn.dto.auth.*;
import com._xdev.satorn.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

  private final AuthenticationService authenticationService;

  /**
   * Register new user
   * POST /api/auth/register
   */
  @PostMapping("/register")
  public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
    try {
      authenticationService.registerUser(request);
      log.info("User registered successfully: {}", request.getUsername());
      return ResponseEntity.ok(new MessageResponse("User registered successfully"));
    } catch (RuntimeException e) {
      log.error("Registration failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Registration failed: " + e.getMessage()));
    }
  }

  /**
   * Authenticate user and generate tokens
   * POST /api/auth/login
   */
  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    try {
      LoginResponse response = authenticationService.login(request);
      log.info("User logged in: {}", request.getUsername());
      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      log.error("Login failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Login failed: " + e.getMessage()));
    }
  }

  /**
   * Refresh access token
   * POST /api/auth/refresh
   */
  @PostMapping("/refresh")
  public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
    try {
      LoginResponse response = authenticationService.refreshToken(request.getRefreshToken());
      log.info("Token refreshed successfully");
      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      log.error("Token refresh failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Token refresh failed: " + e.getMessage()));
    }
  }

  /**
   * Logout user (revoke refresh token)
   * POST /api/auth/logout
   */
  @PostMapping("/logout")
  public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
    try {
      authenticationService.logout(request.getRefreshToken());
      log.info("User logged out successfully");
      return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    } catch (RuntimeException e) {
      log.error("Logout failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(new MessageResponse("Logout failed: " + e.getMessage()));
    }
  }
}
