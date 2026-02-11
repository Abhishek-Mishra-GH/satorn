package com._xdev.satorn.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtils jwtUtils;
  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    try {
      // Extract JWT from request header
      String jwt = parseJwt(request);

      if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
        // Get username from JWT
        String username = jwtUtils.getUsernameFromJwtToken(jwt);

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Create authentication token
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities());

        // Set additional details
        authentication.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request));

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Set authentication for user: {}", username);
      }
    } catch (Exception e) {
      log.error("Cannot set user authentication: {}", e.getMessage());
    }

    // Continue filter chain
    filterChain.doFilter(request, response);
  }

  /**
   * Extract JWT token from Authorization header
   */
  private String parseJwt(HttpServletRequest request) {
    String headerAuth = request.getHeader("Authorization");

    if (StringUtils.hasText(headerAuth) && headerAuth.toLowerCase().startsWith("bearer ")) {
      return headerAuth.substring(7); // Remove "Bearer " prefix
    }

    return null;
  }
}
