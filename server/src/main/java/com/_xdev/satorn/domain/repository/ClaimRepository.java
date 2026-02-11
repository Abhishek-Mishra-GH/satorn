package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.Claim;
import com._xdev.satorn.domain.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
  List<Claim> findByArticle(Article article);
}
