package com._xdev.satorn.controller;

import com._xdev.satorn.dto.learning.LearningProfileRequest;
import com._xdev.satorn.dto.learning.QuizGenerateRequest;
import com._xdev.satorn.dto.learning.QuizSubmitRequest;
import com._xdev.satorn.dto.learning.TutorRequest;
import com._xdev.satorn.service.learning.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class LearningController {

  private final LearningService learningService;

  @GetMapping("/profile")
  public ResponseEntity<?> getProfile(Authentication authentication) {
    try {
      return ResponseEntity.ok(learningService.getProfile(authentication.getName()));
    } catch (Exception e) {
      log.error("Failed to get learning profile", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PutMapping("/profile")
  public ResponseEntity<?> upsertProfile(
      @RequestBody LearningProfileRequest request,
      Authentication authentication) {
    try {
      return ResponseEntity.ok(learningService.upsertProfile(authentication.getName(), request));
    } catch (Exception e) {
      log.error("Failed to update learning profile", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/recommendations")
  public ResponseEntity<?> getRecommendations(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication authentication) {
    try {
      return ResponseEntity.ok(learningService.getRecommendations(authentication.getName(), page, size));
    } catch (Exception e) {
      log.error("Failed to get recommendations", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/quiz/generate")
  public ResponseEntity<?> generateQuiz(
      @RequestBody(required = false) QuizGenerateRequest request,
      Authentication authentication) {
    try {
      QuizGenerateRequest safeRequest = request == null ? QuizGenerateRequest.builder().build() : request;
      return ResponseEntity.ok(learningService.generateQuiz(authentication.getName(), safeRequest));
    } catch (Exception e) {
      log.error("Failed to generate quiz", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/quiz/submit")
  public ResponseEntity<?> submitQuiz(
      @Valid @RequestBody QuizSubmitRequest request,
      Authentication authentication) {
    try {
      return ResponseEntity.ok(learningService.submitQuiz(authentication.getName(), request));
    } catch (Exception e) {
      log.error("Failed to submit quiz", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/skills")
  public ResponseEntity<?> getSkills(Authentication authentication) {
    try {
      return ResponseEntity.ok(learningService.getSkills(authentication.getName()));
    } catch (Exception e) {
      log.error("Failed to get skill progress", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/tutor")
  public ResponseEntity<?> askTutor(
      @Valid @RequestBody TutorRequest request,
      Authentication authentication) {
    try {
      return ResponseEntity.ok(learningService.askTutor(authentication.getName(), request));
    } catch (Exception e) {
      log.error("Failed to get tutor response", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
