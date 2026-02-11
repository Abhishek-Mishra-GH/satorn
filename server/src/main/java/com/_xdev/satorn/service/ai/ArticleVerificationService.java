package com._xdev.satorn.service.ai;

import com._xdev.satorn.domain.entity.*;
import com._xdev.satorn.domain.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Orchestrator service that coordinates the entire article verification
 * workflow
 * Manages claim extraction, verification, synthesis, and categorization
 */
@Slf4j
@Service
@Transactional
public class ArticleVerificationService {

  private final ClaimExtractionService claimExtractionService;
  private final VerificationService verificationService;
  private final SynthesisService synthesisService;
  private final CategoryTaggingService categoryTaggingService;
  private final ArticleRepository articleRepository;
  private final ClaimRepository claimRepository;
  private final VerificationRepository verificationRepository;
  private final SynthesisRepository synthesisRepository;
  private final CategoryRepository categoryRepository;

  public ArticleVerificationService(
      ClaimExtractionService claimExtractionService,
      VerificationService verificationService,
      SynthesisService synthesisService,
      CategoryTaggingService categoryTaggingService,
      ArticleRepository articleRepository,
      ClaimRepository claimRepository,
      VerificationRepository verificationRepository,
      SynthesisRepository synthesisRepository,
      CategoryRepository categoryRepository) {
    this.claimExtractionService = claimExtractionService;
    this.verificationService = verificationService;
    this.synthesisService = synthesisService;
    this.categoryTaggingService = categoryTaggingService;
    this.articleRepository = articleRepository;
    this.claimRepository = claimRepository;
    this.verificationRepository = verificationRepository;
    this.synthesisRepository = synthesisRepository;
    this.categoryRepository = categoryRepository;
  }

  /**
   * Full verification pipeline for an article
   * 1. Extract claims
   * 2. Verify each claim
   * 3. Synthesize results
   * 4. Categorize article
   */
  public VerificationResult verifyArticle(Article article) {
    try {
      log.info("Starting full verification for article: {}", article.getTitle());

      VerificationResult result = new VerificationResult();
      result.setArticle(article);

      // Step 1: Extract claims
      log.info("Step 1: Extracting claims...");
      List<Claim> claims = claimExtractionService.extractHighPriorityClaims(
          article.getContent(), 3);

      for (Claim claim : claims) {
        claim.setArticle(article);
      }
      claimRepository.saveAll(claims);
      result.setClaims(claims);
      log.info("Extracted {} claims", claims.size());

      // Step 2: Verify claims
      log.info("Step 2: Verifying claims...");
      List<Verification> verifications = new ArrayList<>();
      for (Claim claim : claims) {
        Verification verification = verificationService.verifyClaim(claim.getText());
        verification.setVerdict(verification.getVerdict() != null ? verification.getVerdict() : "UNVERIFIABLE");
        verifications.add(verification);
      }
      verificationRepository.saveAll(verifications);
      result.setVerifications(verifications);
      log.info("Verified {} claims", verifications.size());

      // Step 3: Synthesize results
      log.info("Step 3: Synthesizing results...");
      Synthesis synthesis = synthesisService.synthesizeVerifications(article, verifications);
      synthesis.setArticle(article);
      synthesisRepository.save(synthesis);
      result.setSynthesis(synthesis);
      log.info("Synthesis complete - Credibility Score: {}", synthesis.getCredibilityScore());

      // Step 4: Categorize article
      log.info("Step 4: Categorizing article...");
      CategoryTaggingService.CategoryTaggingResult categoryResult = categoryTaggingService
          .categorizeArticle(article.getTitle(), article.getContent());

      Category category = new Category();
      category.setName(categoryResult.getPrimaryCategory().getDisplayName());
      category.setColor(categoryResult.getPrimaryCategory().getColor());
      categoryRepository.save(category);
      article.setCategory(category);
      result.setCategory(category);
      log.info("Article categorized as: {}", category.getName());

      // Update article status and credibility
      article.setStatus("VERIFIED");
      article.setCredibilityScore(synthesis.getCredibilityScore());
      article.setUpdatedAt(LocalDateTime.now());
      articleRepository.save(article);

      result.setStatus("COMPLETED");
      log.info("Article verification completed successfully");

      return result;
    } catch (Exception e) {
      log.error("Article verification failed", e);
      VerificationResult result = new VerificationResult();
      result.setArticle(article);
      result.setStatus("FAILED");
      result.setError(e.getMessage());
      return result;
    }
  }

  /**
   * Quick verification (fast claims + verification only, no synthesis)
   */
  public QuickVerificationResult quickVerify(Article article) {
    try {
      log.info("Starting quick verification for article: {}", article.getTitle());

      QuickVerificationResult result = new QuickVerificationResult();

      // Extract high-priority claims only
      List<Claim> claims = claimExtractionService.extractHighPriorityClaims(
          article.getContent(), 5);
      for (Claim claim : claims) {
        claim.setArticle(article);
      }
      claimRepository.saveAll(claims);

      // Quick verify each claim
      List<Verification> verifications = verificationService.verifyMultipleClaims(
          claims.stream().map(Claim::getText).toList());
      verificationRepository.saveAll(verifications);

      // Calculate overall score
      double overallScore = calculateOverallCredibility(verifications);
      article.setCredibilityScore(overallScore);
      article.setStatus("QUICK_VERIFIED");
      article.setUpdatedAt(LocalDateTime.now());
      articleRepository.save(article);

      result.setArticle(article);
      result.setVerifications(verifications);
      result.setOverallCredibility(overallScore);

      return result;
    } catch (Exception e) {
      log.error("Quick verification failed", e);
      return new QuickVerificationResult();
    }
  }

  /**
   * Get verification progress for an article
   */
  public VerificationProgress getVerificationProgress(Long articleId) {
    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> new IllegalArgumentException("Article not found"));

    VerificationProgress progress = new VerificationProgress();
    progress.setArticleTitle(article.getTitle());
    progress.setStatus(article.getStatus());

    List<Claim> claims = claimRepository.findByArticle(article);
    progress.setTotalClaims(claims.size());

    long verifiedCount = claims.stream()
        .filter(c -> c.getVerification() != null)
        .count();
    progress.setVerifiedClaims(verifiedCount);

    if (article.getCredibilityScore() != null) {
      progress.setCredibilityScore(article.getCredibilityScore());
    }

    return progress;
  }

  /**
   * Calculate overall credibility from verifications
   */
  private double calculateOverallCredibility(List<Verification> verifications) {
    if (verifications.isEmpty()) {
      return 50.0;
    }

    double totalScore = 0;
    for (Verification v : verifications) {
      totalScore += v.getConfidence();
    }

    return totalScore / verifications.size();
  }

  /**
   * Verification result wrapper
   */
  @lombok.Data
  public static class VerificationResult {
    private Article article;
    private List<Claim> claims;
    private List<Verification> verifications;
    private Synthesis synthesis;
    private Category category;
    private String status = "PENDING";
    private String error;
  }

  /**
   * Quick verification result (lightweight)
   */
  @lombok.Data
  public static class QuickVerificationResult {
    private Article article;
    private List<Verification> verifications;
    private double overallCredibility;
  }

  /**
   * Verification progress model
   */
  @lombok.Data
  public static class VerificationProgress {
    private String articleTitle;
    private String status;
    private int totalClaims;
    private long verifiedClaims;
    private double credibilityScore;

    public int getProgressPercentage() {
      if (totalClaims == 0)
        return 0;
      return (int) (verifiedClaims * 100 / totalClaims);
    }
  }
}
