package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.ChatSession;
import com._xdev.satorn.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
  List<ChatSession> findByUserOrderByCreatedAtDesc(User user);

  @Query("SELECT s FROM ChatSession s JOIN FETCH s.user u WHERE s.id = :sessionId AND u.username = :username")
  Optional<ChatSession> findOwnedSessionWithUser(
      @Param("sessionId") Long sessionId,
      @Param("username") String username);
}
