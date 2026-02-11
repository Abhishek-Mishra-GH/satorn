package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_skill_progress", uniqueConstraints = {
    @UniqueConstraint(name = "uk_learning_skill_user_category", columnNames = { "user_id", "category_name" })
}, indexes = {
    @Index(name = "idx_learning_skill_user", columnList = "user_id"),
    @Index(name = "idx_learning_skill_category", columnList = "category_name"),
    @Index(name = "idx_learning_skill_mastery", columnList = "mastery_score")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningSkillProgress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "category_name", nullable = false, length = 120)
  private String categoryName;

  @Builder.Default
  @Column(name = "mastery_score", nullable = false)
  private Double masteryScore = 0.0;

  @Builder.Default
  @Column(name = "attempted_questions", nullable = false)
  private Integer attemptedQuestions = 0;

  @Builder.Default
  @Column(name = "correct_answers", nullable = false)
  private Integer correctAnswers = 0;

  @Column(name = "last_practiced_at")
  private LocalDateTime lastPracticedAt;

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
    if (masteryScore == null) {
      masteryScore = 0.0;
    }
    if (attemptedQuestions == null) {
      attemptedQuestions = 0;
    }
    if (correctAnswers == null) {
      correctAnswers = 0;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
