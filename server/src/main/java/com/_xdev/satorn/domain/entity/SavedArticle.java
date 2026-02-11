package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_articles", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "user_id", "article_id" })
}, indexes = {
    @Index(name = "idx_saved_user", columnList = "user_id"),
    @Index(name = "idx_saved_article", columnList = "article_id"),
    @Index(name = "idx_saved_saved_at", columnList = "saved_at"),
    @Index(name = "idx_saved_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedArticle {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "article_id", nullable = false)
  private SynthesizedArticle article;

  @Column(name = "saved_at", nullable = false)
  private LocalDateTime savedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (savedAt == null) {
      savedAt = LocalDateTime.now();
    }
    if (createdAt == null) {
      createdAt = savedAt;
    }
  }
}
