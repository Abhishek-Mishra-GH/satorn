package com._xdev.satorn.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com._xdev.satorn.ai.config.LLMFactory;
import com._xdev.satorn.ai.prompt.PromptTemplates;
import com._xdev.satorn.ai.util.ResponseParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for categorizing and tagging articles
 */
@Slf4j
@Service
public class CategoryTaggingService {

  private final LLMFactory llmFactory;

  public CategoryTaggingService(LLMFactory llmFactory) {
    this.llmFactory = llmFactory;
  }

  public enum ArticleCategory {
    POLITICS("Politics", "#FF6B6B"),
    HEALTH("Health & Science", "#4ECDC4"),
    TECHNOLOGY("Technology", "#45B7D1"),
    ECONOMY("Economy", "#FFA07A"),
    SOCIETY("Society", "#98D8C8"),
    ENVIRONMENT("Environment", "#6BCB77"),
    SPORTS("Sports", "#FFD93D"),
    ENTERTAINMENT("Entertainment", "#FF69B4"),
    SECURITY("Security", "#8B4513"),
    MEDIA("Media", "#696969");

    private final String displayName;
    private final String color;

    ArticleCategory(String displayName, String color) {
      this.displayName = displayName;
      this.color = color;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getColor() {
      return color;
    }
  }

  /**
   * Categorize article based on content
   */
  public CategoryTaggingResult categorizeArticle(String articleTitle, String articleContent) {
    try {
      log.info("Categorizing article: {}", articleTitle);

      ChatClient client = llmFactory.getClientForTask(LLMFactory.TaskType.CATEGORY_TAGGING);

      StringBuilder categoriesText = new StringBuilder();
      for (ArticleCategory cat : ArticleCategory.values()) {
        categoriesText.append("- ").append(cat.getDisplayName()).append("\n");
      }

      String prompt = PromptTemplates.CATEGORY_TAGGING_PROMPT
          .replace("{article}", articleTitle + "\n" + articleContent)
          .replace("{categories}", categoriesText.toString());

      String response = client.prompt()
          .user(prompt)
          .call()
          .content();

      return parseCategorizationResponse(response);
    } catch (Exception e) {
      log.error("Failed to categorize article", e);
      return new CategoryTaggingResult();
    }
  }

  /**
   * Parse categorization response from LLM
   */
  private CategoryTaggingResult parseCategorizationResponse(String response) {
    try {
      JsonNode json = ResponseParser.parseJsonNode(response);

      CategoryTaggingResult result = new CategoryTaggingResult();

      // Get primary category
      String primaryCat = json.get("primary_category").asText("SOCIETY");
      try {
        result.setPrimaryCategory(ArticleCategory.valueOf(primaryCat.toUpperCase().replace(" ", "_")));
      } catch (IllegalArgumentException e) {
        result.setPrimaryCategory(ArticleCategory.SOCIETY);
      }

      // Get tags
      JsonNode tagsNode = json.get("tags");
      if (tagsNode != null && tagsNode.isArray()) {
        for (JsonNode tag : tagsNode) {
          result.getTags().add(tag.asText());
        }
      }

      return result;
    } catch (Exception e) {
      log.error("Failed to parse categorization response", e);
      return new CategoryTaggingResult();
    }
  }

  /**
   * Suggest tags based on article content
   */
  public List<String> suggestTags(String content, int maxTags) {
    try {
      ChatClient client = llmFactory.getClientForTask(LLMFactory.TaskType.CATEGORY_TAGGING);

      String prompt = String.format("""
          Extract the %d most important tags/keywords:
          %s
          Return JSON: {tags: [string]}
          """, maxTags, content);

      String response = client.prompt()
          .user(prompt)
          .call()
          .content();

      JsonNode json = ResponseParser.parseJsonNode(response);
      List<String> tags = new ArrayList<>();

      if (json.has("tags") && json.get("tags").isArray()) {
        for (JsonNode tag : json.get("tags")) {
          tags.add(tag.asText());
        }
      }

      return tags;
    } catch (Exception e) {
      log.error("Failed to suggest tags", e);
      return new ArrayList<>();
    }
  }

  /**
   * Categorization result model
   */
  @lombok.Data
  public static class CategoryTaggingResult {
    private ArticleCategory primaryCategory = ArticleCategory.SOCIETY;
    private Map<ArticleCategory, Double> secondaryCategories = new HashMap<>();
    private List<String> tags = new ArrayList<>();
  }
}
