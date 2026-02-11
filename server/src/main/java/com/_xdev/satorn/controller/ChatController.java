package com._xdev.satorn.controller;

import com._xdev.satorn.dto.ChatRequest;
import com._xdev.satorn.service.ai.ChatService;
import com._xdev.satorn.service.ai.ChatService.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatController {

  private final ChatService chatService;

  @PostMapping
  public ResponseEntity<ChatResponse> chat(
      @RequestBody ChatRequest request,
      Authentication authentication) {

    String username = resolveUsername(authentication);
    log.info("Chat request from: {}", username != null ? username : "guest");

    ChatResponse response = chatService.processMessage(
        request.getSessionId(),
        request.getMessage(),
        username);

    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamChat(
      @RequestBody ChatRequest request,
      Authentication authentication) {
    String username = resolveUsername(authentication);
    log.info("Streaming chat request from: {}", username != null ? username : "guest");
    return chatService.streamMessage(request.getSessionId(), request.getMessage(), username);
  }

  @GetMapping("/sessions/{sessionId}")
  public ResponseEntity<?> getChatHistory(
      @PathVariable Long sessionId,
      Authentication authentication) {

    String username = resolveUsername(authentication);
    if (username == null) {
      return ResponseEntity.ok(List.of());
    }

    var history = chatService.getSessionHistory(sessionId, username).stream()
        .map(msg -> Map.of(
            "id", msg.getId(),
            "role", msg.getRole(),
            "content", msg.getContent(),
            "intent", msg.getIntent(),
            "createdAt", msg.getCreatedAt()))
        .toList();
    return ResponseEntity.ok(history);
  }

  @GetMapping("/sessions")
  public ResponseEntity<?> getMySessions(
      @RequestParam(defaultValue = "10") int limit,
      Authentication authentication) {

    String username = resolveUsername(authentication);
    if (username == null) {
      return ResponseEntity.ok(List.of());
    }

    List<Map<String, Object>> sessions = chatService.getUserSessions(username, limit).stream()
        .map(session -> Map.<String, Object>of(
            "id", session.getId(),
            "title", session.getTitle(),
            "createdAt", session.getCreatedAt(),
            "updatedAt", session.getUpdatedAt(),
            "active", session.isActive()))
        .toList();
    return ResponseEntity.ok(sessions);
  }

  private String resolveUsername(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }

    String name = authentication.getName();
    if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
      return null;
    }

    return name;
  }
}
