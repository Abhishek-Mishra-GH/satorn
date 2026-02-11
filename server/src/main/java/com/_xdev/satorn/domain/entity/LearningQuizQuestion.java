package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_quiz_questions", indexes = {
    @Index(name = "idx_learning_q_question_session", columnList = "session_id"),
    @Index(name = "idx_learning_q_question_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningQuizQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private LearningQuizSession session;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "synthesized_article_id")
  private SynthesizedArticle synthesizedArticle;

  @Column(name = "category", length = 120)
  private String category;

  @Column(name = "difficulty", length = 30)
  private String difficulty;

  @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
  private String questionText;

  @Column(name = "option_a", nullable = false, columnDefinition = "TEXT")
  private String optionA;

  @Column(name = "option_b", nullable = false, columnDefinition = "TEXT")
  private String optionB;

  @Column(name = "option_c", nullable = false, columnDefinition = "TEXT")
  private String optionC;

  @Column(name = "option_d", nullable = false, columnDefinition = "TEXT")
  private String optionD;

  @Column(name = "correct_option", nullable = false, length = 1)
  private String correctOption;

  @Column(name = "explanation", columnDefinition = "TEXT")
  private String explanation;

  @Column(name = "sort_order")
  private Integer sortOrder;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
