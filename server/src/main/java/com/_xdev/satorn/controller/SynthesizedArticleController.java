package com._xdev.satorn.controller;

import com._xdev.satorn.domain.entity.SavedArticle;
import com._xdev.satorn.domain.entity.SynthesizedArticle;
import com._xdev.satorn.domain.entity.User;
import com._xdev.satorn.domain.repository.SavedArticleRepository;
import com._xdev.satorn.domain.repository.SynthesizedArticleRepository;
import com._xdev.satorn.domain.repository.UserRepository;
import com._xdev.satorn.service.ai.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller for synthesized articles from RSS feeds
 * Provides endpoints for viewing synthesized, verified articles
 */
@RestController
@RequestMapping("/api/synthesized-articles")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class SynthesizedArticleController {

  private static final Pattern FIGURE_PATTERN = Pattern.compile(
      "(?i)(?:\\$|USD\\s?|EUR\\s?|GBP\\s?|INR\\s?|RS\\.?\\s?)?\\d[\\d,]*(?:\\.\\d+)?%?");

  private final SynthesizedArticleRepository synthesizedArticleRepository;
  private final SavedArticleRepository savedArticleRepository;
  private final UserRepository userRepository;
  private final RateLimiter rateLimiter;

  /**
   * Get all synthesized articles (paginated)
   */
  @GetMapping
  public ResponseEntity<?> listSynthesizedArticles(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String status) {
    try {
      PageRequest pageable = newestFirstPageable(page, size);
      Page<SynthesizedArticle> articles;

      if (category != null && !category.isEmpty()) {
        articles = synthesizedArticleRepository.findByCategory_NameIgnoreCase(category, pageable);
      } else if (status != null && !status.isEmpty()) {
        articles = synthesizedArticleRepository.findByStatus(status, pageable);
      } else {
        articles = synthesizedArticleRepository.findAll(pageable);
      }

      Map<String, Object> response = new HashMap<>();
      response.put("total", articles.getTotalElements());
      response.put("page", page);
      response.put("size", size);
      response.put("articles", articles.getContent().stream().map(this::mapToDto).toList());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error listing synthesized articles", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get trending synthesized articles
   */
  @GetMapping("/trending")
  public ResponseEntity<?> getTrendingArticles(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    try {
      Page<SynthesizedArticle> articles = synthesizedArticleRepository.findTrendingArticles(newestFirstPageable(page, size));

      Map<String, Object> response = new HashMap<>();
      response.put("total", articles.getTotalElements());
      response.put("articles", articles.getContent().stream().map(this::mapToDto).toList());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error fetching trending articles", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get top verified articles by credibility score
   */
  @GetMapping("/top-credible")
  public ResponseEntity<?> getTopCredibleArticles(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    try {
      Page<SynthesizedArticle> articles = synthesizedArticleRepository.findTopByCredibilityScore(newestFirstPageable(page, size));

      Map<String, Object> response = new HashMap<>();
      response.put("total", articles.getTotalElements());
      response.put("articles", articles.getContent().stream().map(this::mapToDto).toList());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error fetching top credible articles", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Search synthesized articles
   */
  @GetMapping("/search")
  public ResponseEntity<?> searchArticles(
      @RequestParam String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    try {
      Page<SynthesizedArticle> articles = synthesizedArticleRepository.search(query, newestFirstPageable(page, size));

      Map<String, Object> response = new HashMap<>();
      response.put("total", articles.getTotalElements());
      response.put("query", query);
      response.put("articles", articles.getContent().stream().map(this::mapToDto).toList());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error searching articles", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get single synthesized article by ID
   */
  @GetMapping("/{id}")
  public ResponseEntity<?> getSynthesizedArticle(@PathVariable Long id) {
    try {
      SynthesizedArticle article = synthesizedArticleRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Article not found"));

      article.setViewCount((article.getViewCount() != null ? article.getViewCount() : 0) + 1);
      synthesizedArticleRepository.save(article);

      return ResponseEntity.ok(mapToDetailedDto(article));
    } catch (Exception e) {
      log.error("Error fetching synthesized article", e);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get articles by category
   */
  @GetMapping("/category/{category}")
  public ResponseEntity<?> getArticlesByCategory(
      @PathVariable String category,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    try {
      Page<SynthesizedArticle> articles = synthesizedArticleRepository.findByCategory_NameIgnoreCase(
          category, newestFirstPageable(page, size));

      Map<String, Object> response = new HashMap<>();
      response.put("total", articles.getTotalElements());
      response.put("category", category);
      response.put("articles", articles.getContent().stream().map(this::mapToDto).toList());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error fetching articles by category", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Save (like) article for current user.
   */
  @PostMapping("/{id}/save")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<?> saveArticle(@PathVariable Long id, Authentication authentication) {
    return saveArticleInternal(id, authentication);
  }

  /**
   * Alias endpoint for liking article.
   */
  @PostMapping("/{id}/like")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<?> likeArticle(@PathVariable Long id, Authentication authentication) {
    return saveArticleInternal(id, authentication);
  }

  /**
   * Remove saved/liked article for current user.
   */
  @DeleteMapping("/{id}/save")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<?> unsaveArticle(@PathVariable Long id, Authentication authentication) {
    return unsaveArticleInternal(id, authentication);
  }

  /**
   * Alias endpoint for unliking article.
   */
  @DeleteMapping("/{id}/like")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<?> unlikeArticle(@PathVariable Long id, Authentication authentication) {
    return unsaveArticleInternal(id, authentication);
  }

  /**
   * Get current user's saved articles.
   */
  @GetMapping("/saved")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<?> getSavedArticles(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication authentication) {
    try {
      User user = resolveUser(authentication);
      Page<SavedArticle> savedPage = savedArticleRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(page, size));

      Map<String, Object> response = new HashMap<>();
      response.put("total", savedPage.getTotalElements());
      response.put("page", page);
      response.put("size", size);
      response.put("articles", savedPage.getContent().stream().map(saved -> {
        Map<String, Object> dto = mapToDto(saved.getArticle());
        dto.put("savedAt", saved.getSavedAt());
        return dto;
      }).toList());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error fetching saved articles", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get article statistics (admin only)
   */
  @GetMapping("/admin/statistics")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> getStatistics() {
    try {
      Map<String, Object> stats = new HashMap<>();
      stats.put("totalArticles", synthesizedArticleRepository.count());
      stats.put("verifiedArticles", synthesizedArticleRepository.countByStatus("VERIFIED"));
      stats.put("mostlyTrueArticles", synthesizedArticleRepository.countByVerdict("MOSTLY_TRUE"));
      stats.put("mostlyFalseArticles", synthesizedArticleRepository.countByVerdict("MOSTLY_FALSE"));
      stats.put("mixedVerdict", synthesizedArticleRepository.countByVerdict("MIXED"));
      stats.put("unverifiableArticles", synthesizedArticleRepository.countByVerdict("UNVERIFIABLE"));

      return ResponseEntity.ok(stats);
    } catch (Exception e) {
      log.error("Error fetching statistics", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get rate limiter status (admin only)
   */
  @GetMapping("/admin/rate-limiter-status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> getRateLimiterStatus() {
    try {
      return ResponseEntity.ok(rateLimiter.getStatus());
    } catch (Exception e) {
      log.error("Error fetching rate limiter status", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  private ResponseEntity<?> saveArticleInternal(Long id, Authentication authentication) {
    try {
      User user = resolveUser(authentication);
      SynthesizedArticle article = synthesizedArticleRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Article not found"));

      if (savedArticleRepository.existsByUserAndArticle(user, article)) {
        return ResponseEntity.ok(Map.of("message", "Article already saved", "saved", true));
      }

      SavedArticle saved = SavedArticle.builder()
          .user(user)
          .article(article)
          .build();
      savedArticleRepository.save(saved);

      return ResponseEntity.ok(Map.of("message", "Article saved", "saved", true));
    } catch (Exception e) {
      log.error("Error saving article", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  private ResponseEntity<?> unsaveArticleInternal(Long id, Authentication authentication) {
    try {
      User user = resolveUser(authentication);
      SynthesizedArticle article = synthesizedArticleRepository.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Article not found"));

      savedArticleRepository.findByUserAndArticle(user, article)
          .ifPresent(savedArticleRepository::delete);

      return ResponseEntity.ok(Map.of("message", "Article removed from saved list", "saved", false));
    } catch (Exception e) {
      log.error("Error unsaving article", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  private User resolveUser(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null
        || authentication.getName().isBlank() || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
      throw new IllegalStateException("Authentication required");
    }

    return userRepository.findByUsername(authentication.getName())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
  }

  /**
   * Map article to DTO (list view)
   */
  private Map<String, Object> mapToDto(SynthesizedArticle article) {
    Map<String, Object> dto = new HashMap<>();
    dto.put("id", article.getId());
    dto.put("title", article.getTitle());
    dto.put("sourceUrl", article.getSourceUrl());
    dto.put("rssFeedSource", article.getRssFeedSource());
    dto.put("author", article.getAuthor());
    dto.put("credibilityScore", article.getCredibilityScore());
    dto.put("verdict", article.getVerdict());
    dto.put("category", article.getCategory() != null ? article.getCategory().getName() : null);
    dto.put("claimsCount", article.getClaimsCount());
    dto.put("trueClaims", article.getTrueClaims());
    dto.put("falseClaims", article.getFalseClaims());
    dto.put("imageUrl", article.getImageUrl());
    dto.put("viewCount", article.getViewCount());
    dto.put("isTrending", article.getIsTrending());
    dto.put("createdAt", article.getCreatedAt());
    return dto;
  }

  /**
   * Map article to detailed DTO (detail view)
   */
  private Map<String, Object> mapToDetailedDto(SynthesizedArticle article) {
    Map<String, Object> dto = mapToDto(article);
    dto.put("synthesizedNarrative", article.getSynthesizedNarrative());
    dto.put("keyFindings", article.getKeyFindings());
    dto.put("timeline", article.getTimeline());
    dto.put("originalContent", article.getOriginalContent());
    dto.put("publishDate", article.getPublishDate());
    dto.put("verifiedClaimsCount", article.getVerifiedClaimsCount());
    dto.put("unverifiableClaims", article.getUnverifiableClaims());
    dto.put("status", article.getStatus());
    dto.put("keyFindingsBullets", toBullets(article.getKeyFindings()));
    dto.put("timelineEvents", parseTimelineEvents(article));
    dto.put("explainLikeIm5", buildExplainLikeIm5(article));
    dto.put("whyItMatters", buildWhyItMatters(article));
    dto.put("whatToWatch", buildWhatToWatch(article));
    dto.put("keyFigures", extractKeyFigures(article));
    dto.put("quickStats", buildQuickStats(article));
    dto.put("readingTimeMinutes", estimateReadingTimeMinutes(article));
    dto.put("discussionPrompts", buildDiscussionPrompts(article));
    return dto;
  }

  private List<String> toBullets(String raw) {
    List<String> bullets = new ArrayList<>();
    if (raw == null || raw.isBlank()) {
      return bullets;
    }
    for (String line : raw.split("\\r?\\n")) {
      String trimmed = line.trim();
      if (trimmed.isBlank()) {
        continue;
      }
      trimmed = trimmed.replaceFirst("^[\\p{Punct}\\p{S}]+\\s*", "");
      if (!trimmed.isBlank()) {
        bullets.add(trimmed);
      }
    }
    return bullets;
  }

  private List<Map<String, String>> parseTimelineEvents(SynthesizedArticle article) {
    String timeline = article.getTimeline();
    List<Map<String, String>> events = new ArrayList<>();
    if (timeline == null || timeline.isBlank()) {
      return buildFallbackTimelineEvents(article);
    }
    for (String line : timeline.split("\\r?\\n")) {
      String trimmed = line.trim();
      if (trimmed.isBlank() || trimmed.equalsIgnoreCase("Timeline of Events:")) {
        continue;
      }

      String date = null;
      String event = trimmed;
      if (trimmed.startsWith("[") && trimmed.contains("]")) {
        int end = trimmed.indexOf(']');
        if (end > 1) {
          date = trimmed.substring(1, end).trim();
          event = trimmed.substring(end + 1).trim();
        }
      }
      if (event.startsWith("-")) {
        event = event.substring(1).trim();
      }

      Map<String, String> item = new HashMap<>();
      item.put("date", date);
      item.put("event", event);
      events.add(item);
    }
    return events.isEmpty() ? buildFallbackTimelineEvents(article) : events;
  }

  private List<Map<String, String>> buildFallbackTimelineEvents(SynthesizedArticle article) {
    List<Map<String, String>> events = new ArrayList<>();
    if (article.getPublishDate() != null) {
      events.add(newTimelineEvent(article.getPublishDate().toLocalDate().toString(), "Source article published"));
    }
    if (article.getCreatedAt() != null) {
      events.add(newTimelineEvent(article.getCreatedAt().toLocalDate().toString(), "Article entered verification pipeline"));
    }
    if (safeInt(article.getVerifiedClaimsCount()) > 0) {
      events.add(newTimelineEvent(
          article.getCreatedAt() == null ? null : article.getCreatedAt().toLocalDate().toString(),
          "Verification completed for " + safeInt(article.getVerifiedClaimsCount()) + " claims"));
    }
    if (events.isEmpty()) {
      events.add(newTimelineEvent(null, "Timeline is being assembled from article and verification data."));
    }
    return events;
  }

  private Map<String, String> newTimelineEvent(String date, String event) {
    Map<String, String> item = new HashMap<>();
    item.put("date", date);
    item.put("event", event);
    return item;
  }

  private String buildExplainLikeIm5(SynthesizedArticle article) {
    String core = firstNonBlank(article.getSynthesizedNarrative(), article.getKeyFindings(), article.getTitle());
    String sentence = firstSentence(core);
    if (sentence.isBlank()) {
      sentence = "A news story was checked by comparing claims with available evidence.";
    }
    String verdictHint = switch (safeUpper(article.getVerdict())) {
      case "MOSTLY_TRUE", "VERIFIED", "TRUE" -> "Most checked claims look reliable right now.";
      case "MOSTLY_FALSE", "FALSE", "CONTRADICTED" -> "Several checked claims do not match the evidence.";
      case "UNVERIFIABLE" -> "There is not enough trustworthy evidence yet.";
      default -> "Some claims are supported, and some are still uncertain.";
    };
    return sentence + " " + verdictHint;
  }

  private String buildWhyItMatters(SynthesizedArticle article) {
    String category = article.getCategory() != null ? article.getCategory().getName() : "general news";
    String verdict = safeUpper(article.getVerdict());
    if ("MOSTLY_FALSE".equals(verdict) || "FALSE".equals(verdict) || "CONTRADICTED".equals(verdict)) {
      return "This story matters because misinformation in " + category.toLowerCase()
          + " can spread quickly and influence decisions before facts are checked.";
    }
    if ("UNVERIFIABLE".equals(verdict)) {
      return "This story matters because key facts are still unclear, so readers should wait for stronger evidence.";
    }
    return "This story matters because verified context in " + category.toLowerCase()
        + " helps readers understand what is likely true and what still needs scrutiny.";
  }

  private List<String> buildWhatToWatch(SynthesizedArticle article) {
    List<String> watch = new ArrayList<>();
    if (safeInt(article.getUnverifiableClaims()) > 0) {
      watch.add("Watch for independent reports that confirm currently unverifiable claims.");
    }
    if (safeInt(article.getFalseClaims()) > 0) {
      watch.add("Track official corrections or retractions related to disputed claims.");
    }
    if (article.getPublishDate() != null && Duration.between(article.getPublishDate(), LocalDateTime.now()).toHours() < 48) {
      watch.add("This is a developing story, so key details may still change in the next 24-48 hours.");
    }
    if (watch.isEmpty()) {
      watch.add("Look for fresh evidence, primary sources, and follow-up reporting.");
    }
    return watch;
  }

  private List<Map<String, String>> extractKeyFigures(SynthesizedArticle article) {
    String corpus = String.join(" ",
        defaultIfBlank(article.getTitle(), ""),
        defaultIfBlank(article.getSynthesizedNarrative(), ""),
        defaultIfBlank(article.getKeyFindings(), ""),
        defaultIfBlank(article.getOriginalContent(), ""));

    Set<String> unique = new LinkedHashSet<>();
    Matcher matcher = FIGURE_PATTERN.matcher(corpus);
    while (matcher.find() && unique.size() < 8) {
      String value = matcher.group().trim();
      if (value.length() > 1) {
        unique.add(value);
      }
    }

    List<Map<String, String>> figures = new ArrayList<>();
    for (String value : unique) {
      int index = corpus.indexOf(value);
      int start = Math.max(0, index - 35);
      int end = Math.min(corpus.length(), index + value.length() + 35);
      String context = corpus.substring(start, end).replaceAll("\\s+", " ").trim();
      Map<String, String> item = new HashMap<>();
      item.put("value", value);
      item.put("context", context);
      figures.add(item);
    }
    return figures;
  }

  private Map<String, Object> buildQuickStats(SynthesizedArticle article) {
    int total = Math.max(0, safeInt(article.getClaimsCount()));
    int trueClaims = Math.max(0, safeInt(article.getTrueClaims()));
    int falseClaims = Math.max(0, safeInt(article.getFalseClaims()));
    int unverifiable = Math.max(0, safeInt(article.getUnverifiableClaims()));
    int verified = Math.max(0, safeInt(article.getVerifiedClaimsCount()));

    Map<String, Object> stats = new HashMap<>();
    stats.put("claimsAnalyzed", total);
    stats.put("claimsVerified", verified);
    stats.put("trueClaims", trueClaims);
    stats.put("falseClaims", falseClaims);
    stats.put("unverifiableClaims", unverifiable);
    stats.put("credibilityScore", article.getCredibilityScore());
    stats.put("confidenceLevel", confidenceLabel(article.getCredibilityScore()));
    if (total > 0) {
      stats.put("trueRatio", Math.round((trueClaims * 100.0) / total));
      stats.put("falseRatio", Math.round((falseClaims * 100.0) / total));
    } else {
      stats.put("trueRatio", 0);
      stats.put("falseRatio", 0);
    }
    return stats;
  }

  private int estimateReadingTimeMinutes(SynthesizedArticle article) {
    String text = firstNonBlank(article.getSynthesizedNarrative(), article.getOriginalContent(), article.getTitle());
    if (text == null || text.isBlank()) {
      return 1;
    }
    int words = text.trim().split("\\s+").length;
    return Math.max(1, (int) Math.ceil(words / 200.0));
  }

  private List<String> buildDiscussionPrompts(SynthesizedArticle article) {
    List<String> prompts = new ArrayList<>();
    prompts.add("What part of this story has the strongest supporting evidence?");
    prompts.add("Which claim needs more independent confirmation?");
    if (article.getCategory() != null) {
      prompts.add("How could this impact " + article.getCategory().getName().toLowerCase() + " in the next week?");
    }
    return prompts;
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }

  private String firstSentence(String text) {
    if (text == null || text.isBlank()) {
      return "";
    }
    String cleaned = text.replaceAll("\\s+", " ").trim();
    int boundary = cleaned.indexOf(". ");
    if (boundary > 0) {
      return cleaned.substring(0, boundary + 1).trim();
    }
    return cleaned.length() > 220 ? cleaned.substring(0, 220).trim() + "..." : cleaned;
  }

  private String confidenceLabel(Double score) {
    if (score == null) {
      return "Unknown";
    }
    if (score >= 80) {
      return "High";
    }
    if (score >= 60) {
      return "Moderate";
    }
    if (score >= 40) {
      return "Mixed";
    }
    return "Low";
  }

  private int safeInt(Number value) {
    return value == null ? 0 : value.intValue();
  }

  private String safeUpper(String value) {
    return value == null ? "" : value.toUpperCase();
  }

  private String defaultIfBlank(String value, String fallback) {
    return (value == null || value.isBlank()) ? fallback : value;
  }

  private PageRequest newestFirstPageable(int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(size, 100));
    return PageRequest.of(
        safePage,
        safeSize,
        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
  }
}
