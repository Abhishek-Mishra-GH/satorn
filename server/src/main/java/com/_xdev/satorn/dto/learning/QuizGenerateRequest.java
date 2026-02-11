package com._xdev.satorn.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizGenerateRequest {

  private Integer questionCount;
  private String category;
  private String difficulty;
}
