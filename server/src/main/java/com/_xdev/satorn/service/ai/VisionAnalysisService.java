package com._xdev.satorn.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com._xdev.satorn.ai.config.LLMFactory;
import com._xdev.satorn.ai.util.ResponseParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for verifying images and memes
 */
@Slf4j
@Service
public class VisionAnalysisService {

  private final LLMFactory llmFactory;

  public VisionAnalysisService(LLMFactory llmFactory) {
    this.llmFactory = llmFactory;
  }

  /**
   * Analyze image/meme for fact-checking
   */
  public VisionAnalysisResult analyzeImage(String imageUrl, String context) {
    try {
      log.info("Analyzing image: {}", imageUrl);

      ChatClient client = llmFactory.getClientForTask(LLMFactory.TaskType.VISION);

      String prompt = """
          Analyze this image for fact-checking:
          URL: {url}
          Context: {context}
          Extract: text content, claims, manipulation flags
          Return JSON: {text_content: string, claims: [], manipulation_flags: [], confidence: number}
          """.replace("{url}", imageUrl).replace("{context}", context);

      String response = client.prompt().user(prompt).call().content();
      return parseVisionResponse(response);
    } catch (Exception e) {
      log.error("Failed to analyze image", e);
      return new VisionAnalysisResult();
    }
  }

  /**
   * Detect if image has been manipulated
   */
  public ManipulationDetectionResult detectManipulation(String imageUrl) {
    try {
      ChatClient client = llmFactory.getClientForTask(LLMFactory.TaskType.VISION);

      String prompt = """
          Detect image manipulation:
          URL: {url}
          Return JSON: {is_manipulated: boolean, confidence: number, issues: []}
          """.replace("{url}", imageUrl);

      String response = client.prompt().user(prompt).call().content();
      return parseManipulationResponse(response);
    } catch (Exception e) {
      log.error("Failed to detect manipulation", e);
      return new ManipulationDetectionResult();
    }
  }

  /**
   * Extract text from image
   */
  public String extractTextFromImage(String imageUrl) {
    try {
      ChatClient client = llmFactory.getClientForTask(LLMFactory.TaskType.VISION);

      String prompt = String.format("Extract ALL text from this image:\n%s", imageUrl);

      return client.prompt().user(prompt).call().content();
    } catch (Exception e) {
      log.error("Failed to extract text", e);
      return "";
    }
  }

  private VisionAnalysisResult parseVisionResponse(String response) {
    try {
      JsonNode json = ResponseParser.parseJsonNode(response);
      VisionAnalysisResult result = new VisionAnalysisResult();
      result.setTextContent(json.get("text_content").asText(""));
      result.setConfidence(json.get("confidence").asDouble(0.0));

      JsonNode claimsNode = json.get("claims");
      if (claimsNode != null && claimsNode.isArray()) {
        for (JsonNode claim : claimsNode) {
          result.getClaims().add(claim.asText());
        }
      }

      return result;
    } catch (Exception e) {
      return new VisionAnalysisResult();
    }
  }

  private ManipulationDetectionResult parseManipulationResponse(String response) {
    try {
      JsonNode json = ResponseParser.parseJsonNode(response);
      ManipulationDetectionResult result = new ManipulationDetectionResult();
      result.setManipulated(json.get("is_manipulated").asBoolean(false));
      result.setConfidence(json.get("confidence").asDouble(0.0));
      return result;
    } catch (Exception e) {
      return new ManipulationDetectionResult();
    }
  }

  @lombok.Data
  public static class VisionAnalysisResult {
    private String textContent;
    private List<String> claims = new ArrayList<>();
    private double confidence;
  }

  @lombok.Data
  public static class ManipulationDetectionResult {
    private boolean isManipulated;
    private double confidence;
    private List<String> issues = new ArrayList<>();
  }
}
