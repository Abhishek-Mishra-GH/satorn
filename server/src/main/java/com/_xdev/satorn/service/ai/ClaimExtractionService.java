package com._xdev.satorn.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com._xdev.satorn.ai.config.LLMFactory;
import com._xdev.satorn.ai.prompt.PromptTemplates;
import com._xdev.satorn.ai.util.ResponseParser;
import com._xdev.satorn.domain.entity.Claim;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for extracting individual factual claims from articles
 * Uses AI to identify, classify, and prioritize claims for verification
 */
@Slf4j
@Service
public class ClaimExtractionService {

  private final LLMFactory llmFactory;
  private final ObjectMapper objectMapper;

  @Value("${ai.claim-extraction.max-input-chars:6000}")
  private int maxInputChars;

  @Value("${ai.claim-extraction.timeout-seconds:90}")
  private int timeoutSeconds;

  public ClaimExtractionService(LLMFactory llmFactory, ObjectMapper objectMapper) {
    this.llmFactory = llmFactory;
    this.objectMapper = objectMapper;
  }

  /**
   * Extract claims from article text
   */
  public List<Claim> extractClaims(String articleText) {
    try {
      int originalLength = articleText == null ? 0 : articleText.length();
      String safeArticle = sanitizeForExtraction(articleText);
      log.info("Extracting claims from article (length: {}, effectiveLength: {})", originalLength, safeArticle.length());

      ChatClient client = llmFactory.getClientForTask(LLMFactory.TaskType.CLAIM_EXTRACTION);

      String prompt = PromptTemplates.CLAIM_EXTRACTION_PROMPT
          .replace("{article}", safeArticle);

      String response = CompletableFuture.supplyAsync(() -> client.prompt()
          .user(prompt)
          .call()
          .content())
          .orTimeout(Math.max(5, timeoutSeconds), TimeUnit.SECONDS)
          .join();

      return parseClaimsFromResponse(response);
    } catch (Exception e) {
      log.error("Failed to extract claims from article", e);
      return new ArrayList<>();
    }
  }

  /**
   * Parse claims from LLM response
   */
  private List<Claim> parseClaimsFromResponse(String response) {
    try {
      JsonNode json = ResponseParser.parseJsonNode(response);
      List<Claim> claims = new ArrayList<>();

      if (json.isArray()) {
        for (JsonNode node : json) {
          Claim claim = new Claim();
          claim.setText(node.get("claim").asText());
          claim.setType(node.get("type").asText());
          Integer importance = node.get("importance").asInt(5);
          claim.setImportance(importance.toString());
          claim.setCreatedAt(java.time.LocalDateTime.now());
          claims.add(claim);
        }
      }

      return claims;
    } catch (Exception e) {
      log.error("Failed to parse claims from response: {}", response, e);
      return new ArrayList<>();
    }
  }

  /**
   * Extract claims by type
   */
  public List<Claim> extractClaimsByType(String articleText, String claimType) {
    List<Claim> allClaims = extractClaims(articleText);
    return allClaims.stream()
        .filter(c -> c.getType().equals(claimType))
        .toList();
  }

  /**
   * Extract high-priority claims (importance >= threshold)
   */
  public List<Claim> extractHighPriorityClaims(String articleText, int importanceThreshold) {
    List<Claim> allClaims = extractClaims(articleText);
    return allClaims.stream()
        .filter(c -> {
          try {
            return Integer.parseInt(c.getImportance()) >= importanceThreshold;
          } catch (NumberFormatException e) {
            return false;
          }
        })
        .sorted((c1, c2) -> {
          try {
            return Integer.compare(Integer.parseInt(c2.getImportance()),
                Integer.parseInt(c1.getImportance()));
          } catch (NumberFormatException e) {
            return 0;
          }
        })
        .toList();
  }

  private String sanitizeForExtraction(String text) {
    if (text == null) {
      return "";
    }
    String normalized = text.replaceAll("\\s+", " ").trim();
    int limit = Math.max(500, maxInputChars);
    if (normalized.length() <= limit) {
      return normalized;
    }
    return normalized.substring(0, limit);
  }

  /**
   * Refine extracted claims with better wording
   */
  public String refineClaim(String claim) {
    try {
      ChatClient client = llmFactory.getClientForTask(LLMFactory.TaskType.CLAIM_EXTRACTION);

      String response = client.prompt()
          .user("Rephrase this claim to be clear, concise, and testable: \"" + claim + "\"")
          .call()
          .content();

      return response.trim();
    } catch (Exception e) {
      log.error("Failed to refine claim: {}", claim, e);
      return claim;
    }
  }

  /**
   * Cluster related claims
   */
  public Map<String, List<Claim>> clusterRelatedClaims(List<Claim> claims) {
    try {
      Map<String, List<Claim>> clusters = new HashMap<>();

      ChatClient client = llmFactory.getClientForTask(LLMFactory.TaskType.CLAIM_EXTRACTION);

      StringBuilder claimsText = new StringBuilder();
      for (int i = 0; i < claims.size(); i++) {
        claimsText.append(i).append(". ").append(claims.get(i).getText()).append("\n");
      }

      String prompt = "Group these claims into logical clusters (claims about same topic/event):\n" +
          claimsText +
          "\nReturn JSON: {clusters: [{topic: string, claim_indices: [int]}]}";

      String response = client.prompt()
          .user(prompt)
          .call()
          .content();

      JsonNode json = ResponseParser.parseJsonNode(response);
      JsonNode clusterNodes = json.get("clusters");

      if (clusterNodes != null && clusterNodes.isArray()) {
        for (JsonNode clusterNode : clusterNodes) {
          String topic = clusterNode.get("topic").asText("Unknown");
          List<Claim> clusterClaims = new ArrayList<>();

          for (JsonNode indexNode : clusterNode.get("claim_indices")) {
            int idx = indexNode.asInt();
            if (idx < claims.size()) {
              clusterClaims.add(claims.get(idx));
            }
          }

          clusters.put(topic, clusterClaims);
        }
      }

      return clusters;
    } catch (Exception e) {
      log.error("Failed to cluster claims", e);
      return new HashMap<>();
    }
  }
}
