package com._xdev.satorn.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_message_session", columnList = "session_id"),
    @Index(name = "idx_chat_message_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", nullable = false)
  private ChatSession session;

  @Column(nullable = false, length = 50)
  private String role;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "message_type", length = 50)
  @Builder.Default
  private String messageType = "TEXT";

  @Column(name = "image_url", length = 2048)
  private String imageUrl;

  @Column(length = 50)
  private String intent;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
}
