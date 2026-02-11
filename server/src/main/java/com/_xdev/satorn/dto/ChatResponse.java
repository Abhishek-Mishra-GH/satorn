package com._xdev.satorn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
  private Long sessionId;
  private String message;
  private String intent;
  private String messageType;
}
