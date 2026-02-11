package com._xdev.satorn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleRequest {
  @NotBlank
  private String url;

  @NotBlank
  private String title;

  @NotBlank
  private String content;

  private String imageUrl;
  private String author;
  private String source;
}
