package com._xdev.satorn.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningProfileRequest {

  private String examTrack;
  private LocalDate targetExamDate;
  private Integer dailyStudyMinutes;
  private String preferredDifficulty;
  private List<String> weakCategories;
  private List<String> strongCategories;
  private String learningGoals;
}
