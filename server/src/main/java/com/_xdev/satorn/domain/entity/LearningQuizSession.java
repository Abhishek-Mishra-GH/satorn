package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "learning_quiz_sessions", indexes = {
    @Index(name = "idx_learning_quiz_user", columnList = "user_id"),
    @Index(name = "idx_learning_quiz_status", columnList = "status"),
    @Index(name = "idx_learning_quiz_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningQuizSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "focus_category", length = 120)
  private String focusCategory;

  @Builder.Default
  @Column(name = "difficulty", length = 30)
  private String difficulty = "INTERMEDIATE";

  @Builder.Default
  @Column(name = "status", length = 30)
  private String status = "GENERATED";

  @Builder.Default
  @Column(name = "total_questions")
  private Integer totalQuestions = 0;

  @Builder.Default
  @Column(name = "correct_answers")
  private Integer correctAnswers = 0;

  @Builder.Default
  @Column(name = "score_percent")
  private Double scorePercent = 0.0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<LearningQuizQuestion> questions = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
    if (totalQuestions == null) {
      totalQuestions = 0;
    }
    if (correctAnswers == null) {
      correctAnswers = 0;
    }
    if (scorePercent == null) {
      scorePercent = 0.0;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
