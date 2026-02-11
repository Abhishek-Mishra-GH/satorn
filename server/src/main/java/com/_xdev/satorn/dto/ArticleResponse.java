package com._xdev.satorn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {
  private Long id;
  private String url;
  private String title;
  private String status;
  private Double credibilityScore;
  private String submittedBy;
  private LocalDateTime submittedAt;
  private LocalDateTime verifiedAt;
}
