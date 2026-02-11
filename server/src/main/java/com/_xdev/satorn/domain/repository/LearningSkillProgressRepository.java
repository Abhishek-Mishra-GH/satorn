package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.LearningSkillProgress;
import com._xdev.satorn.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningSkillProgressRepository extends JpaRepository<LearningSkillProgress, Long> {

  List<LearningSkillProgress> findByUserOrderByMasteryScoreAsc(User user);

  Optional<LearningSkillProgress> findByUserAndCategoryNameIgnoreCase(User user, String categoryName);
}
