package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.ChatMessage;
import com._xdev.satorn.domain.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);

  List<ChatMessage> findTop20BySessionOrderByCreatedAtDesc(ChatSession session);
}
