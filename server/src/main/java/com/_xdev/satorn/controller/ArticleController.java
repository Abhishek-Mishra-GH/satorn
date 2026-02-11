package com._xdev.satorn.controller;

import com._xdev.satorn.domain.entity.Article;
import com._xdev.satorn.domain.entity.User;
import com._xdev.satorn.domain.repository.ArticleRepository;
import com._xdev.satorn.domain.repository.UserRepository;
import com._xdev.satorn.dto.ArticleRequest;
import com._xdev.satorn.dto.ArticleResponse;
import com._xdev.satorn.service.ai.ArticleVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ArticleController {

  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;
  private final ArticleVerificationService articleVerificationService;

  /**
   * Submit a new article for verification
   */
  @PostMapping
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<?> submitArticle(
      @Valid @RequestBody ArticleRequest request,
      Authentication authentication) {
    try {
      log.info("Article submission from: {}", authentication.getName());

      // Get the authenticated user from repository
      User user = userRepository.findByUsername(authentication.getName())
          .orElseThrow(() -> new IllegalArgumentException("User not found"));

      // Create article with all required fields
      Article article = Article.builder()
          .title(request.getTitle())
          .content(request.getContent())
          .url(request.getUrl())
          .author(request.getAuthor())
          .status("PENDING")
          .submittedBy(user)
          .submittedAt(LocalDateTime.now())
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      Article savedArticle = articleRepository.save(article);

      Map<String, Object> response = new HashMap<>();
      response.put("id", savedArticle.getId());
      response.put("title", savedArticle.getTitle());
      response.put("status", savedArticle.getStatus());
      response.put("createdAt", savedArticle.getCreatedAt());

      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
      log.error("Error submitting article", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get article by ID
   */
  @GetMapping("/{id}")
  public ResponseEntity<?> getArticle(@PathVariable Long id) {
    try {
      Article article = articleRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Article not found"));

      Map<String, Object> response = new HashMap<>();
      response.put("id", article.getId());
      response.put("title", article.getTitle());
      response.put("content", article.getContent());
      response.put("status", article.getStatus());
      response.put("credibilityScore", article.getCredibilityScore());
      response.put("category", article.getCategory() != null ? article.getCategory().getName() : null);
      response.put("createdAt", article.getCreatedAt());
      response.put("updatedAt", article.getUpdatedAt());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error fetching article", e);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * List articles with pagination
   */
  @GetMapping
  public ResponseEntity<?> listArticles(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    try {
      Page<Article> articles = articleRepository.findAll(PageRequest.of(page, size));

      Map<String, Object> response = new HashMap<>();
      response.put("total", articles.getTotalElements());
      response.put("page", page);
      response.put("size", size);
      response.put("articles", articles.getContent().stream().map(article -> {
        Map<String, Object> articleMap = new HashMap<>();
        articleMap.put("id", article.getId());
        articleMap.put("title", article.getTitle());
        articleMap.put("status", article.getStatus());
        articleMap.put("credibilityScore", article.getCredibilityScore());
        articleMap.put("createdAt", article.getCreatedAt());
        return articleMap;
      }).toList());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error listing articles", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Verify article (full pipeline: claims + verification + synthesis +
   * categorization)
   */
  @PostMapping("/{id}/verify")
  @PreAuthorize("hasRole('MODERATOR')")
  public ResponseEntity<?> verifyArticle(@PathVariable Long id) {
    try {
      log.info("Initiating article verification for article ID: {}", id);

      Article article = articleRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Article not found"));

      ArticleVerificationService.VerificationResult result = articleVerificationService.verifyArticle(article);

      Map<String, Object> response = new HashMap<>();
      response.put("id", result.getArticle().getId());
      response.put("title", result.getArticle().getTitle());
      response.put("status", result.getStatus());
      response.put("credibilityScore", result.getArticle().getCredibilityScore());
      response.put("claimsCount", result.getClaims() != null ? result.getClaims().size() : 0);
      response.put("verificationsCount", result.getVerifications() != null ? result.getVerifications().size() : 0);
      response.put("category", result.getCategory() != null ? result.getCategory().getName() : null);

      if (result.getSynthesis() != null) {
        response.put("summary", result.getSynthesis().getSummary());
        response.put("verdict", result.getSynthesis().getOverallVerdict());
      }

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error verifying article", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Quick verify article (fast claims + verification only)
   */
  @PostMapping("/{id}/quick-verify")
  @PreAuthorize("hasRole('MODERATOR')")
  public ResponseEntity<?> quickVerifyArticle(@PathVariable Long id) {
    try {
      log.info("Initiating quick verification for article ID: {}", id);

      Article article = articleRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Article not found"));

      ArticleVerificationService.QuickVerificationResult result = articleVerificationService.quickVerify(article);

      Map<String, Object> response = new HashMap<>();
      response.put("id", result.getArticle().getId());
      response.put("title", result.getArticle().getTitle());
      response.put("status", result.getArticle().getStatus());
      response.put("credibilityScore", result.getOverallCredibility());
      response.put("verificationsCount", result.getVerifications() != null ? result.getVerifications().size() : 0);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error quick verifying article", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get verification progress for an article
   */
  @GetMapping("/{id}/verification-progress")
  public ResponseEntity<?> getVerificationProgress(@PathVariable Long id) {
    try {
      ArticleVerificationService.VerificationProgress progress = articleVerificationService.getVerificationProgress(id);

      Map<String, Object> response = new HashMap<>();
      response.put("articleTitle", progress.getArticleTitle());
      response.put("status", progress.getStatus());
      response.put("totalClaims", progress.getTotalClaims());
      response.put("verifiedClaims", progress.getVerifiedClaims());
      response.put("progressPercentage", progress.getProgressPercentage());
      response.put("credibilityScore", progress.getCredibilityScore());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error getting verification progress", e);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Delete an article
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> deleteArticle(@PathVariable Long id) {
    try {
      Article article = articleRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Article not found"));

      articleRepository.delete(article);

      return ResponseEntity.ok(Map.of("message", "Article deleted successfully"));
    } catch (Exception e) {
      log.error("Error deleting article", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
