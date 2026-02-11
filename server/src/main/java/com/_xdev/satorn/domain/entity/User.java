package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "username"),
    @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"password", "roles", "submittedArticles", "chatSessions", "refreshTokens"})
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  @Column(nullable = false, length = 50)
  private String username;

  @Column(nullable = false, length = 100)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(name = "first_name", length = 50)
  private String firstName;

  @Column(name = "last_name", length = 50)
  private String lastName;

  @Builder.Default
  @Column(nullable = false)
  private boolean enabled = true;

  @Builder.Default
  @Column(name = "account_non_expired", nullable = false)
  private boolean accountNonExpired = true;

  @Builder.Default
  @Column(name = "account_non_locked", nullable = false)
  private boolean accountNonLocked = true;

  @Builder.Default
  @Column(name = "credentials_non_expired", nullable = false)
  private boolean credentialsNonExpired = true;

  @Builder.Default
  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts = 0;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @Builder.Default
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Builder.Default
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
  @Builder.Default
  private Set<Role> roles = new HashSet<>();

  @OneToMany(mappedBy = "submittedBy", cascade = CascadeType.ALL)
  private Set<Article> submittedArticles;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
  private Set<ChatSession> chatSessions;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
  private Set<RefreshToken> refreshTokens;

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Helper methods for role management
  public void addRole(Role role) {
    if (roles == null) {
      roles = new HashSet<>();
    }
    if (role.getUsers() == null) {
      role.setUsers(new HashSet<>());
    }
    roles.add(role);
    role.getUsers().add(this);
  }

  public void removeRole(Role role) {
    if (roles == null || role.getUsers() == null) {
      return;
    }
    roles.remove(role);
    role.getUsers().remove(this);
  }

  public boolean hasRole(String roleName) {
    return roles.stream()
        .anyMatch(role -> role.getName().equals(roleName));
  }

  // Account security methods
  public void incrementFailedAttempts() {
    this.failedLoginAttempts++;
  }

  public void resetFailedAttempts() {
    this.failedLoginAttempts = 0;
  }

  public void lock() {
    this.accountNonLocked = false;
  }

  public void unlock() {
    this.accountNonLocked = true;
    this.failedLoginAttempts = 0;
  }
}
