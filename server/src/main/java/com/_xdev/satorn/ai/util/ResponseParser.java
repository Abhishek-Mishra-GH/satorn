package com._xdev.satorn.ai.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing LLM responses (typically JSON)
 */
@Slf4j
public class ResponseParser {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```",
      Pattern.CASE_INSENSITIVE);

  /**
   * Extract JSON from response text (handles markdown code blocks)
   */
  public static String extractJson(String response) {
    if (response == null) {
      return "";
    }

    Matcher matcher = CODE_FENCE_PATTERN.matcher(response);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }

    String trimmed = response.trim();
    if (trimmed.isEmpty()) {
      return trimmed;
    }

    int firstObject = trimmed.indexOf('{');
    int firstArray = trimmed.indexOf('[');
    int firstIdx;
    if (firstObject < 0) {
      firstIdx = firstArray;
    } else if (firstArray < 0) {
      firstIdx = firstObject;
    } else {
      firstIdx = Math.min(firstObject, firstArray);
    }

    if (firstIdx < 0) {
      return trimmed;
    }

    char start = trimmed.charAt(firstIdx);
    char end = start == '[' ? ']' : '}';
    int lastIdx = trimmed.lastIndexOf(end);
    if (lastIdx > firstIdx) {
      return trimmed.substring(firstIdx, lastIdx + 1).trim();
    }
    return trimmed.substring(firstIdx).trim();
  }

  /**
   * Parse JSON response to Map
   */
  public static Map<String, Object> parseJsonResponse(String response) {
    try {
      JsonNode node = parseJsonNode(response);
      if (!node.isObject()) {
        return new HashMap<>();
      }
      return mapper.convertValue(node, Map.class);
    } catch (Exception e) {
      log.error("Failed to parse JSON response: {}", preview(response), e);
      return new HashMap<>();
    }
  }

  /**
   * Parse JSON response to JsonNode for flexible access
   */
  public static JsonNode parseJsonNode(String response) {
    String json = extractJson(response);
    if (json.isBlank()) {
      return mapper.createObjectNode();
    }

    try {
      return mapper.readTree(json);
    } catch (Exception e) {
      String repaired = repairCommonJsonIssues(json);
      if (!repaired.equals(json)) {
        try {
          return mapper.readTree(repaired);
        } catch (Exception retry) {
          log.error("Failed to parse repaired JSON node: {}", preview(repaired), retry);
        }
      }
      log.error("Failed to parse JSON node: {}", preview(response), e);
      return mapper.createObjectNode();
    }
  }

  /**
   * Repair common LLM JSON formatting issues, especially multiline text inside quoted fields.
   */
  private static String repairCommonJsonIssues(String json) {
    String normalized = json.replace("\uFEFF", "").trim();
    return escapeNewlinesInsideStrings(normalized);
  }

  /**
   * Escapes raw newlines and tabs that appear inside string literals.
   * This keeps narrative blocks parseable without altering JSON structure.
   */
  private static String escapeNewlinesInsideStrings(String input) {
    StringBuilder out = new StringBuilder(input.length() + 64);
    boolean inString = false;
    boolean escaped = false;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      if (escaped) {
        out.append(c);
        escaped = false;
        continue;
      }

      if (c == '\\') {
        out.append(c);
        escaped = true;
        continue;
      }

      if (c == '"') {
        inString = !inString;
        out.append(c);
        continue;
      }

      if (inString) {
        if (c == '\n') {
          out.append("\\n");
          continue;
        }
        if (c == '\r') {
          out.append("\\r");
          continue;
        }
        if (c == '\t') {
          out.append("\\t");
          continue;
        }
      }

      out.append(c);
    }

    return out.toString();
  }

  private static String preview(String text) {
    if (text == null) {
      return "<null>";
    }
    String flattened = text.replaceAll("\\s+", " ").trim();
    int max = 500;
    if (flattened.length() <= max) {
      return flattened;
    }
    return flattened.substring(0, max) + "...";
  }

  /**
   * Safely get string value from parsed response
   */
  public static String getString(Map<String, Object> map, String key, String defaultValue) {
    Object value = map.get(key);
    return value != null ? value.toString() : defaultValue;
  }

  /**
   * Safely get integer value from parsed response
   */
  public static int getInt(Map<String, Object> map, String key, int defaultValue) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    try {
      return value != null ? Integer.parseInt(value.toString()) : defaultValue;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Safely get double value from parsed response
   */
  public static double getDouble(Map<String, Object> map, String key, double defaultValue) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    try {
      return value != null ? Double.parseDouble(value.toString()) : defaultValue;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Convert credibility score to verdict
   */
  public static String scoreToVerdict(double score) {
    if (score >= 80)
      return "VERIFIED";
    if (score >= 60)
      return "MOSTLY_TRUE";
    if (score >= 40)
      return "PARTLY_TRUE";
    if (score >= 20)
      return "MOSTLY_FALSE";
    return "FALSE";
  }

  /**
   * Normalize score to 0-100 range
   */
  public static double normalizeScore(double score) {
    if (score < 0)
      return 0;
    if (score > 100)
      return 100;
    return score;
  }
}
