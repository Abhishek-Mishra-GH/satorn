package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.Article;
import com._xdev.satorn.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
  Page<Article> findBySubmittedBy(User user, Pageable pageable);

  Page<Article> findByStatus(String status, Pageable pageable);
}
