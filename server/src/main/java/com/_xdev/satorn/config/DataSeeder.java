package com._xdev.satorn.config;

import com._xdev.satorn.domain.entity.Category;
import com._xdev.satorn.domain.entity.Role;
import com._xdev.satorn.domain.entity.User;
import com._xdev.satorn.domain.repository.CategoryRepository;
import com._xdev.satorn.domain.repository.RoleRepository;
import com._xdev.satorn.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

  private final RoleRepository roleRepository;
  private final CategoryRepository categoryRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(String... args) {
    seedRoles();
    seedCategories();
    seedUsers();
  }

  private void seedRoles() {
    createRoleIfMissing(Role.ROLE_ADMIN, "Administrator");
    createRoleIfMissing(Role.ROLE_MODERATOR, "Moderator");
    createRoleIfMissing(Role.ROLE_USER, "Standard user");
  }

  private void createRoleIfMissing(String name, String description) {
    if (!roleRepository.existsByName(name)) {
      roleRepository.save(new Role(name, description));
      log.info("Seeded role {}", name);
    }
  }

  private void seedCategories() {
    List<Map.Entry<String, String>> categories = List.of(
        Map.entry("Politics", "#FF6B6B"),
        Map.entry("Health", "#4ECDC4"),
        Map.entry("Technology", "#45B7D1"),
        Map.entry("Economy", "#FFA07A"),
        Map.entry("Society", "#98D8C8"),
        Map.entry("Environment", "#6BCB77"),
        Map.entry("Sports", "#FFB6B9"),
        Map.entry("Entertainment", "#FF8B94"),
        Map.entry("Security", "#AF69EE"),
        Map.entry("Media", "#FFD93D")
    );

    for (Map.Entry<String, String> entry : categories) {
      if (!categoryRepository.findByName(entry.getKey()).isPresent()) {
        Category category = new Category();
        category.setName(entry.getKey());
        category.setColor(entry.getValue());
        categoryRepository.save(category);
        log.info("Seeded category {}", entry.getKey());
      }
    }
  }

  private void seedUsers() {
    Role adminRole = roleRepository.findByName(Role.ROLE_ADMIN)
        .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found during seed"));
    Role userRole = roleRepository.findByName(Role.ROLE_USER)
        .orElseThrow(() -> new IllegalStateException("ROLE_USER not found during seed"));

    createUserIfMissing("admin", "admin.satorn@gmail.com", "Admin", "Satorn", adminRole);
    createUserIfMissing("abhishek", "abhishek.satorn@gmail.com", "Abhishek", "Satorn", userRole);
  }

  private void createUserIfMissing(String username, String email, String firstName, String lastName, Role role) {
    if (userRepository.existsByUsername(username)) {
      return;
    }

    User user = User.builder()
        .username(username)
        .email(email)
        .password(passwordEncoder.encode("12345678"))
        .firstName(firstName)
        .lastName(lastName)
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .build();

    user.addRole(role);
    userRepository.save(user);
    log.info("Seeded user {}", username);
  }
}
