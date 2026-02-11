package com._xdev.satorn.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com._xdev.satorn.ai.config.LLMFactory;
import com._xdev.satorn.ai.prompt.PromptTemplates;
import com._xdev.satorn.ai.util.ResponseParser;
import com._xdev.satorn.domain.entity.Evidence;
import com._xdev.satorn.domain.entity.Verification;
import com._xdev.satorn.service.external.TavilySearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalDateTime;

/**
 * Service for verifying individual claims using evidence from Tavily and RAG
 * database
 */
@Slf4j
@Service
public class VerificationService {

  private final LLMFactory llmFactory;
  private final TavilySearchService tavilySearchService;
  private final RagContextService ragContextService;

  public VerificationService(LLMFactory llmFactory,
      TavilySearchService tavilySearchService,
      RagContextService ragContextService) {
    this.llmFactory = llmFactory;
    this.tavilySearchService = tavilySearchService;
    this.ragContextService = ragContextService;
  }

  /**
   * Verify a single claim using Tavily search and evidence analysis
   */
  public Verification verifyClaim(String claim) {
    try {
      log.info("Verifying claim: {}", claim);

      // Search for evidence
      List<Evidence> evidence = searchForEvidence(claim);
      String ragContext = ragContextService.buildContext(claim, 3);

      if (evidence.isEmpty()) {
        log.warn("No evidence found for claim: {}", claim);
        Verification verification = new Verification();
        verification.setVerdict("UNVERIFIABLE");
        verification.setConfidence(0.0);
        verification.setExplanation("No evidence found to verify or contradict this claim.");
        verification.setCreatedAt(LocalDateTime.now());
        return verification;
      }

      // Analyze evidence and generate verdict
      return analyzeEvidenceAndGenerateVerdict(claim, evidence, ragContext);
    } catch (Exception e) {
      log.error("Failed to verify claim: {}", claim, e);
      Verification verification = new Verification();
      verification.setVerdict("ERROR");
      verification.setConfidence(0.0);
      verification.setExplanation("Error during verification: " + e.getMessage());
      verification.setCreatedAt(LocalDateTime.now());
      return verification;
    }
  }

  /**
   * Search for evidence related to a claim
   */
  private List<Evidence> searchForEvidence(String claim) {
    List<Evidence> evidence = new ArrayList<>();

    try {
      // Search using Tavily
      TavilySearchService.SearchResults results = tavilySearchService.searchForEvidence(claim, 5);

      if (results.getResults() != null) {
        for (TavilySearchService.SearchResult result : results.getResults()) {
          Evidence ev = new Evidence();
          ev.setSourceUrl(result.getUrl());
          ev.setSourceTitle(result.getTitle());
          ev.setSnippet(result.getContent());
          ev.setRelevanceScore(result.getScore() != null ? result.getScore().doubleValue() : 0.5);
          ev.setCreatedAt(LocalDateTime.now());
          evidence.add(ev);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to search for evidence using Tavily", e);
    }

    return evidence;
  }

  /**
   * Analyze evidence and generate verification verdict
   */
  private Verification analyzeEvidenceAndGenerateVerdict(String claim, List<Evidence> evidence, String ragContext) {
    try {
      ChatClient client = llmFactory.getClientForTask(LLMFactory.TaskType.VERIFICATION);

      // Format evidence for prompt
      StringBuilder evidenceText = new StringBuilder();
      for (int i = 0; i < evidence.size(); i++) {
        evidenceText.append(i + 1).append(". ").append(evidence.get(i).getSnippet())
            .append(" (Source: ").append(evidence.get(i).getSourceTitle()).append(")\n");
      }

      String prompt = PromptTemplates.VERIFICATION_PROMPT
          .replace("{claim}", claim)
          .replace("{evidence}", evidenceText + "\n\nInternal verified context:\n" +
              (ragContext == null || ragContext.isBlank() ? "None" : ragContext));

      String response = client.prompt()
          .user(prompt)
          .call()
          .content();

      return parseVerificationResponse(response, evidence);
    } catch (Exception e) {
      log.error("Failed to analyze evidence for claim: {}", claim, e);
      Verification verification = new Verification();
      verification.setVerdict("ERROR");
      verification.setConfidence(0.0);
      verification.setExplanation("Error during analysis");
      verification.setCreatedAt(LocalDateTime.now());
      return verification;
    }
  }

  /**
   * Parse verification response from LLM
   */
  private Verification parseVerificationResponse(String response, List<Evidence> evidence) {
    try {
      JsonNode json = ResponseParser.parseJsonNode(response);

      Verification verification = new Verification();
      verification.setVerdict(json.get("verdict").asText("UNVERIFIABLE"));
      verification.setConfidence(json.get("confidence").asDouble(0.0));
      verification.setExplanation(json.get("explanation").asText(""));
      verification.setCreatedAt(LocalDateTime.now());

      // Normalize confidence score
      verification.setConfidence(ResponseParser.normalizeScore(verification.getConfidence()));

      return verification;
    } catch (Exception e) {
      log.error("Failed to parse verification response", e);
      Verification verification = new Verification();
      verification.setVerdict("ERROR");
      verification.setConfidence(0.0);
      verification.setExplanation("Failed to parse verification results");
      verification.setCreatedAt(LocalDateTime.now());
      return verification;
    }
  }

  /**
   * Batch verify multiple claims
   */
  public List<Verification> verifyMultipleClaims(List<String> claims) {
    return claims.stream()
        .map(this::verifyClaim)
        .toList();
  }

  /**
   * Calculate overall credibility score from multiple verifications
   */
  public double calculateCredibilityScore(List<Verification> verifications) {
    if (verifications.isEmpty()) {
      return 0.0;
    }

    double totalScore = 0;
    int verifiableCount = 0;

    for (Verification v : verifications) {
      if (!v.getVerdict().equals("UNVERIFIABLE") && !v.getVerdict().equals("ERROR")) {
        String verdict = v.getVerdict();
        double score = switch (verdict) {
          case "VERIFIED" -> 100.0;
          case "PARTIALLY_VERIFIED" -> 75.0;
          case "MOSTLY_TRUE" -> 70.0;
          case "PARTLY_TRUE" -> 50.0;
          case "MOSTLY_FALSE" -> 30.0;
          case "CONTRADICTED" -> 0.0;
          default -> v.getConfidence();
        };

        totalScore += score * (v.getConfidence() / 100.0);
        verifiableCount++;
      }
    }

    return verifiableCount > 0 ? totalScore / verifiableCount : 50.0;
  }
}
