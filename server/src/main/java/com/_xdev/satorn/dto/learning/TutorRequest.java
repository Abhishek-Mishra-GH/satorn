package com._xdev.satorn.dto.learning;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorRequest {

  @NotBlank
  private String question;

  private Long contextArticleId;
}
