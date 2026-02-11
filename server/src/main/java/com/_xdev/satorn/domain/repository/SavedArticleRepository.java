package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.SavedArticle;
import com._xdev.satorn.domain.entity.SynthesizedArticle;
import com._xdev.satorn.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedArticleRepository extends JpaRepository<SavedArticle, Long> {

  boolean existsByUserAndArticle(User user, SynthesizedArticle article);

  Optional<SavedArticle> findByUserAndArticle(User user, SynthesizedArticle article);

  Page<SavedArticle> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
