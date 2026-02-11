package com._xdev.satorn.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com._xdev.satorn.ai.config.LLMFactory;
import com._xdev.satorn.ai.prompt.PromptTemplates;
import com._xdev.satorn.ai.util.ResponseParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for building comprehensive timelines from articles and verification
 * results
 */
@Slf4j
@Service
public class TimelineBuilderService {

  private final LLMFactory llmFactory;

  public TimelineBuilderService(LLMFactory llmFactory) {
    this.llmFactory = llmFactory;
  }

  /**
   * Extract timeline events from article and verifications
   */
  public List<TimelineEvent> extractTimeline(List<String> events) {
    List<TimelineEvent> timelineEvents = new ArrayList<>();

    try {
      ChatClient client = llmFactory.getClientForTask(LLMFactory.TaskType.CLAIM_EXTRACTION);

      String eventsText = String.join("\n", events);
      String prompt = "Extract temporal events and dates:\n" + eventsText +
          "\nReturn JSON: {events: [{event: string, date: 'YYYY-MM-DD'}]}";

      String response = client.prompt()
          .user(prompt)
          .call()
          .content();

      JsonNode json = ResponseParser.parseJsonNode(response);
      JsonNode eventsNode = json.get("events");

      if (eventsNode != null && eventsNode.isArray()) {
        for (JsonNode node : eventsNode) {
          TimelineEvent event = new TimelineEvent();
          event.setEvent(node.get("event").asText());
          event.setDate(LocalDateTime.now());
          timelineEvents.add(event);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to extract timeline", e);
    }

    return timelineEvents;
  }

  /**
   * Build narrative timeline description
   */
  public String buildTimelineNarrative(List<TimelineEvent> events) {
    StringBuilder narrative = new StringBuilder("Timeline of Events:\n\n");

    for (TimelineEvent event : events) {
      narrative.append(String.format("[%s] %s\n",
          event.getDate().toLocalDate(),
          event.getEvent()));
    }

    return narrative.toString();
  }

  /**
   * Timeline event model
   */
  @lombok.Data
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  public static class TimelineEvent {
    private String event;
    private LocalDateTime date;
    private String source;
    private String verification;
  }
}
