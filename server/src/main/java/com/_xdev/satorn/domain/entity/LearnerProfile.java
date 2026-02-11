package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "learner_profiles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_learner_profile_user", columnNames = "user_id")
}, indexes = {
    @Index(name = "idx_learner_profile_user", columnList = "user_id"),
    @Index(name = "idx_learner_profile_exam_track", columnList = "exam_track")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearnerProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "exam_track", length = 120)
  private String examTrack;

  @Column(name = "target_exam_date")
  private LocalDate targetExamDate;

  @Column(name = "daily_study_minutes")
  private Integer dailyStudyMinutes;

  @Builder.Default
  @Column(name = "preferred_difficulty", length = 30)
  private String preferredDifficulty = "INTERMEDIATE";

  @Column(name = "weak_categories", columnDefinition = "TEXT")
  private String weakCategoriesCsv;

  @Column(name = "strong_categories", columnDefinition = "TEXT")
  private String strongCategoriesCsv;

  @Column(name = "learning_goals", columnDefinition = "TEXT")
  private String learningGoals;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
