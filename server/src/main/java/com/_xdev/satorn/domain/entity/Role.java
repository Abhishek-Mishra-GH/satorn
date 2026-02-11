package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"users"})
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Builder.Default
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @ManyToMany(mappedBy = "roles")
  @Builder.Default
  private Set<User> users = new HashSet<>();

  // Role constants
  public static final String ROLE_USER = "ROLE_USER";
  public static final String ROLE_MODERATOR = "ROLE_MODERATOR";
  public static final String ROLE_ADMIN = "ROLE_ADMIN";

  public Role(String name, String description) {
    this.name = name;
    this.description = description;
    this.createdAt = LocalDateTime.now();
    this.users = new HashSet<>();
  }
}
