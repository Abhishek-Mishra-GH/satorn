package com._xdev.satorn.dto.learning;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitRequest {

  @NotNull
  private Long quizSessionId;

  @Builder.Default
  private List<AnswerSubmission> answers = new ArrayList<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AnswerSubmission {
    @NotNull
    private Long questionId;
    private String selectedOption;
  }
}
