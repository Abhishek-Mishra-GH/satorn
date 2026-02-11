package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.LearningQuizSession;
import com._xdev.satorn.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearningQuizSessionRepository extends JpaRepository<LearningQuizSession, Long> {

  Optional<LearningQuizSession> findByIdAndUser(Long id, User user);

  Page<LearningQuizSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
