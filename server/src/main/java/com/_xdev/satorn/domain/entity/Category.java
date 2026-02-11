package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(length = 50)
  private String color;

  @OneToMany(mappedBy = "category")
  @Builder.Default
  private List<Article> articles = new ArrayList<>();

  @Column(name = "created_at", updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
}
