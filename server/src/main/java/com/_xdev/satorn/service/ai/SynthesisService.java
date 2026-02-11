package com._xdev.satorn.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com._xdev.satorn.ai.config.LLMFactory;
import com._xdev.satorn.ai.prompt.PromptTemplates;
import com._xdev.satorn.ai.util.ResponseParser;
import com._xdev.satorn.domain.entity.Article;
import com._xdev.satorn.domain.entity.Synthesis;
import com._xdev.satorn.domain.entity.Verification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Locale;

/**
 * Service for synthesizing verification results into engaging narratives
 */
@Slf4j
@Service
public class SynthesisService {

  private final LLMFactory llmFactory;
  private final TimelineBuilderService timelineBuilder;

  public SynthesisService(LLMFactory llmFactory, TimelineBuilderService timelineBuilder) {
    this.llmFactory = llmFactory;
    this.timelineBuilder = timelineBuilder;
  }

  /**
   * Synthesize verification results into comprehensive narrative
   */
  public Synthesis synthesizeVerifications(Article article, List<Verification> verifications) {
    double credibilityScore = calculateOverallCredibility(verifications);
    try {
      log.info("Synthesizing verifications for article: {}", article.getTitle());

      // Build narrative
      String narrative = generateNarrative(article, verifications, credibilityScore);

      // Extract key findings
      List<String> keyFindings = extractKeyFindings(verifications);

      // Create synthesis
      Synthesis synthesis = new Synthesis();
      synthesis.setArticle(article);
      synthesis.setOverallVerdict(scoreToVerdict(credibilityScore));
      synthesis.setCredibilityScore(credibilityScore);
      synthesis.setSummary(narrative);
      synthesis.setKeyFindings(String.join("\n", keyFindings));
//      synthesis.setStatistics(generateStatistics(verifications));
      synthesis.setCreatedAt(LocalDateTime.now());

      return synthesis;
    } catch (Exception e) {
      log.error("Failed to synthesize verifications for article", e);
      Synthesis fallback = new Synthesis();
      fallback.setArticle(article);
      fallback.setOverallVerdict(scoreToVerdict(credibilityScore));
      fallback.setCredibilityScore(credibilityScore);
      fallback.setSummary("Narrative generation failed. Please review claim-level verifications.");
      fallback.setKeyFindings("Claim-level verification details are available.");
      fallback.setCreatedAt(LocalDateTime.now());
      return fallback;
    }
  }

  /**
   * Generate engaging narrative from verification results
   */
  private String generateNarrative(Article article, List<Verification> verifications,
      double credibilityScore) {
    try {
      ChatClient client = llmFactory.getClientForTask(LLMFactory.TaskType.SYNTHESIS);

      // Format verifications for prompt
      StringBuilder verificationText = new StringBuilder();
      for (Verification v : verifications) {
        String claimText = v.getClaim() != null ? v.getClaim().getText() : "Unknown claim";
        double confidence = v.getConfidence() != null ? v.getConfidence() : 0.0;
        verificationText.append("- Claim: ").append(claimText)
            .append("\n  Verdict: ").append(v.getVerdict())
            .append(" (").append(String.format("%.0f%%", confidence))
            .append(")\n  Explanation: ").append(v.getExplanation()).append("\n");
      }

      String prompt = PromptTemplates.SYNTHESIS_PROMPT
          .replace("{article}", article.getTitle() + "\n" + article.getContent())
          .replace("{verifications}", verificationText.toString())
          .replace("{relatedContext}", "");

      String response = client.prompt()
          .user(prompt)
          .call()
          .content();

      return parseNarrativeFromResponse(response);
    } catch (Exception e) {
      log.error("Failed to generate narrative", e);
      return "Unable to generate narrative from model response.";
    }
  }

  private String parseNarrativeFromResponse(String response) {
    if (response == null || response.isBlank()) {
      return "Unable to generate narrative.";
    }

    try {
      JsonNode json = ResponseParser.parseJsonNode(response);
      String narrative = json.path("narrative").asText("");
      if (narrative != null && !narrative.isBlank()) {
        return narrative.trim();
      }

      // Handle variants where model uses different key names.
      for (String key : List.of("Narrative", "summary", "Summary")) {
        String value = json.path(key).asText("");
        if (!value.isBlank()) {
          return value.trim();
        }
      }
    } catch (Exception ignored) {
      // Fall through to plain-text parsing.
    }

    String fromSection = extractSection(
        response,
        "Narrative:",
        List.of("Timeline:", "Key Findings:", "Credibility Score:", "Information Gaps:", "Uncertainties:"));
    if (!fromSection.isBlank()) {
      return fromSection.trim();
    }

    return response.trim();
  }

  private String extractSection(String text, String startLabel, List<String> endLabels) {
    String lower = text.toLowerCase(Locale.ROOT);
    String start = startLabel.toLowerCase(Locale.ROOT);
    int startIdx = lower.indexOf(start);
    if (startIdx < 0) {
      return "";
    }

    int contentStart = startIdx + start.length();
    int endIdx = text.length();
    for (String label : endLabels) {
      int idx = lower.indexOf(label.toLowerCase(Locale.ROOT), contentStart);
      if (idx >= 0 && idx < endIdx) {
        endIdx = idx;
      }
    }

    return text.substring(contentStart, endIdx).trim();
  }

  /**
   * Extract key findings from verifications
   */
  private List<String> extractKeyFindings(List<Verification> verifications) {
    List<String> findings = new ArrayList<>();

    for (Verification v : verifications) {
      if (v.getExplanation() != null && !v.getExplanation().isEmpty()) {
        findings.add("• " + v.getClaim() + ": " + v.getExplanation());
      }
    }

    return findings;
  }

  /**
   * Generate statistics about the verification
   */
  private String generateStatistics(List<Verification> verifications) {
    try {
      long verified = verifications.stream()
          .filter(v -> v.getVerdict().equals("VERIFIED")).count();
      long contradicted = verifications.stream()
          .filter(v -> v.getVerdict().equals("CONTRADICTED")).count();
      long unverifiable = verifications.stream()
          .filter(v -> v.getVerdict().equals("UNVERIFIABLE")).count();

      String stats = String.format("""
          Verification Statistics:
          - Total Claims: %d
          - Verified: %d
          - Contradicted: %d
          - Unverifiable: %d
          - Average Confidence: %.1f%%
          """,
          verifications.size(),
          verified,
          contradicted,
          unverifiable,
          verifications.stream().mapToDouble(Verification::getConfidence).average().orElse(0.0));

      return stats;
    } catch (Exception e) {
      log.error("Failed to generate statistics", e);
      return "Statistics unavailable";
    }
  }

  /**
   * Calculate overall credibility from individual verification scores
   */
  private double calculateOverallCredibility(List<Verification> verifications) {
    if (verifications.isEmpty()) {
      return 50.0;
    }

    double totalScore = 0;
    int weightedCount = 0;

    for (Verification v : verifications) {
      String verdict = v.getVerdict();
      double score = switch (verdict) {
        case "VERIFIED" -> 100.0;
        case "PARTIALLY_VERIFIED" -> 75.0;
        case "MOSTLY_TRUE" -> 70.0;
        case "PARTLY_TRUE" -> 50.0;
        case "MOSTLY_FALSE" -> 30.0;
        case "CONTRADICTED" -> 0.0;
        case "UNVERIFIABLE" -> 50.0;
        default -> 50.0;
      };

      totalScore += score * (v.getConfidence() / 100.0);
      weightedCount++;
    }

    return totalScore / Math.max(weightedCount, 1);
  }

  /**
   * Convert credibility score to verdict
   */
  private String scoreToVerdict(double score) {
    if (score >= 80)
      return "CREDIBLE";
    if (score >= 60)
      return "MOSTLY_CREDIBLE";
    if (score >= 40)
      return "PARTIALLY_CREDIBLE";
    if (score >= 20)
      return "MOSTLY_UNRELIABLE";
    return "NOT_CREDIBLE";
  }
}
