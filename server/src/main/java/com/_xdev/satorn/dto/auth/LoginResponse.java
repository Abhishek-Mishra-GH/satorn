package com._xdev.satorn.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

  private String accessToken;
  private String refreshToken;
  private String tokenType = "Bearer";
  private Long id;
  private String username;
  private String email;
  private List<String> roles;
}
