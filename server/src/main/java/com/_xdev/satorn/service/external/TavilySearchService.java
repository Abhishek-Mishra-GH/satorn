package com._xdev.satorn.service.external;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for integrating with Tavily Search API for fact-checking evidence
 * gathering
 */
@Slf4j
@Service
public class TavilySearchService {

  @Value("${tavily.api-key}")
  private String apiKey;

  @Value("${tavily.base-url:https://api.tavily.com}")
  private String baseUrl;

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public TavilySearchService(RestTemplate restTemplate, ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
  }

  /**
   * Search for evidence related to a claim
   */
  public SearchResults searchForEvidence(String claim, int maxResults) {
    try {
      log.info("Searching Tavily for evidence: {}", claim);

      Map<String, Object> request = new HashMap<>();
      request.put("api_key", apiKey);
      request.put("query", claim);
      request.put("max_results", maxResults);
      request.put("include_images", false);
      request.put("include_answer", true);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(
          baseUrl + "/search",
          entity,
          String.class);

      return parseSearchResults(response.getBody());
    } catch (Exception e) {
      log.error("Failed to search Tavily for evidence: {}", claim, e);
      return new SearchResults();
    }
  }

  /**
   * Advanced search with specific parameters
   */
  public SearchResults advancedSearch(String query, int maxResults, String topic) {
    try {
      log.info("Advanced Tavily search - Query: {}, Topic: {}", query, topic);

      Map<String, Object> request = new HashMap<>();
      request.put("api_key", apiKey);
      request.put("query", query);
      request.put("max_results", maxResults);
      request.put("topic", topic); // "general", "news"
      request.put("include_images", false);
      request.put("include_answer", true);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(
          baseUrl + "/search",
          entity,
          String.class);

      return parseSearchResults(response.getBody());
    } catch (Exception e) {
      log.error("Advanced search failed for query: {}", query, e);
      return new SearchResults();
    }
  }

  /**
   * Search for recent news on a topic
   */
  public SearchResults searchRecentNews(String topic, int maxResults) {
    return advancedSearch(topic, maxResults, "news");
  }

  /**
   * Parse Tavily API response
   */
  private SearchResults parseSearchResults(String responseBody) {
    try {
      Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
      SearchResults results = new SearchResults();

      if (response.containsKey("answer")) {
        results.setAnswer((String) response.get("answer"));
      }

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> results_list = (List<Map<String, Object>>) response.get("results");

      if (results_list != null) {
        results.setResults(results_list.stream()
            .map(this::mapToSearchResult)
            .collect(Collectors.toList()));
      }

      return results;
    } catch (Exception e) {
      log.error("Failed to parse Tavily search results", e);
      return new SearchResults();
    }
  }

  /**
   * Map API result to SearchResult object
   */
  private SearchResult mapToSearchResult(Map<String, Object> resultMap) {
    SearchResult result = new SearchResult();
    result.setTitle((String) resultMap.get("title"));
    result.setUrl((String) resultMap.get("url"));
    result.setContent((String) resultMap.get("content"));
    result.setScore((Number) resultMap.getOrDefault("score", 0));
    return result;
  }

  /**
   * Check if API is configured
   */
  public boolean isConfigured() {
    return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("${tavily.api-key}");
  }

  /**
   * Search results wrapper
   */
  @Data
  public static class SearchResults {
    private String answer;
    private List<SearchResult> results;

    public SearchResults() {
      this.results = List.of();
    }
  }

  /**
   * Individual search result
   */
  @Data
  public static class SearchResult {
    private String title;
    private String url;
    private String content;
    private Number score;
  }
}
