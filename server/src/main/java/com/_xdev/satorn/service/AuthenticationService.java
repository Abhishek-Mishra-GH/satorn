package com._xdev.satorn.service;

import com._xdev.satorn.domain.entity.RefreshToken;
import com._xdev.satorn.domain.entity.Role;
import com._xdev.satorn.domain.entity.User;
import com._xdev.satorn.domain.repository.RoleRepository;
import com._xdev.satorn.domain.repository.UserRepository;
import com._xdev.satorn.dto.auth.LoginRequest;
import com._xdev.satorn.dto.auth.LoginResponse;
import com._xdev.satorn.dto.auth.RegisterRequest;
import com._xdev.satorn.security.JwtUtils;
import com._xdev.satorn.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtUtils jwtUtils;
  private final RefreshTokenService refreshTokenService;
  private final UserService userService;

  /**
   * Register new user
   */
  @Transactional
  public void registerUser(RegisterRequest request) {
    // Check if username already exists
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new RuntimeException("Username already in use");
    }

    // Check if email already exists
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("Email already in use");
    }

    // Create new user
    User user = User.builder()
        .username(request.getUsername())
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .build();

    // Assign default ROLE_USER
    Role userRole = roleRepository.findByName("ROLE_USER")
        .orElseThrow(() -> new RuntimeException("Default role not found"));
    user.addRole(userRole);

    userRepository.save(user);
    log.info("User registered successfully: {}", request.getUsername());
  }

  /**
   * Authenticate user and generate tokens
   */
  @Transactional
  public LoginResponse login(LoginRequest request) {
    try {
      // Authenticate user
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              request.getUsername(),
              request.getPassword()));

      // Handle successful login
      userService.handleSuccessfulLogin(request.getUsername());

      // Generate tokens
      String accessToken = jwtUtils.generateJwtToken(authentication);
      User user = userRepository.findByUsername(request.getUsername())
          .orElseThrow(() -> new RuntimeException("User not found"));
      RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

      // Build response
      UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
      List<String> roles = userDetails.getAuthorities().stream()
          .map(auth -> auth.getAuthority())
          .collect(Collectors.toList());

      return LoginResponse.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken.getToken())
          .tokenType("Bearer")
          .id(userDetails.getId())
          .username(userDetails.getUsername())
          .email(userDetails.getEmail())
          .roles(roles)
          .build();

    } catch (Exception e) {
      // Handle failed login
      userService.handleFailedLogin(request.getUsername(), 5);
      log.error("Login failed for user {}: {}", request.getUsername(), e.getMessage());
      throw new RuntimeException("Invalid username or password");
    }
  }

  /**
   * Refresh access token using refresh token
   */
  @Transactional
  public LoginResponse refreshToken(String refreshTokenStr) {
    RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
        .orElseThrow(() -> new RuntimeException("Refresh token not found"));

    // Verify expiration
    refreshTokenService.verifyExpiration(refreshToken);

    // Get user and generate new access token with SHORT expiration (not refresh
    // expiration!)
    User user = refreshToken.getUser();
    String accessToken = jwtUtils.generateTokenFromUsername(user.getUsername(), jwtUtils.getJwtExpiration());

    List<String> roles = user.getRoles().stream()
        .map(Role::getName)
        .collect(Collectors.toList());

    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshTokenStr)
        .tokenType("Bearer")
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .roles(roles)
        .build();
  }

  /**
   * Logout user (revoke refresh token)
   */
  @Transactional
  public void logout(String refreshTokenStr) {
    refreshTokenService.revokeToken(refreshTokenStr);
    log.info("User logged out successfully");
  }
}
