package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.LearningQuizQuestion;
import com._xdev.satorn.domain.entity.LearningQuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningQuizQuestionRepository extends JpaRepository<LearningQuizQuestion, Long> {

  List<LearningQuizQuestion> findBySessionOrderBySortOrderAsc(LearningQuizSession session);
}
