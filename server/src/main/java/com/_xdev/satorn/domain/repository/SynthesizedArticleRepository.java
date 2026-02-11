package com._xdev.satorn.domain.repository;

import com._xdev.satorn.domain.entity.SynthesizedArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SynthesizedArticleRepository extends JpaRepository<SynthesizedArticle, Long> {

  Page<SynthesizedArticle> findByStatus(String status, Pageable pageable);

  Page<SynthesizedArticle> findByCategory_NameIgnoreCase(String categoryName, Pageable pageable);

  @Query("SELECT sa FROM SynthesizedArticle sa WHERE sa.status = 'VERIFIED' ORDER BY sa.credibilityScore DESC, sa.createdAt DESC")
  Page<SynthesizedArticle> findTopByCredibilityScore(Pageable pageable);

  @Query("SELECT sa FROM SynthesizedArticle sa WHERE sa.isTrending = true ORDER BY sa.viewCount DESC, sa.createdAt DESC")
  Page<SynthesizedArticle> findTrendingArticles(Pageable pageable);

  Page<SynthesizedArticle> findByCreatedAtAfter(LocalDateTime date, Pageable pageable);

  @Query("SELECT sa FROM SynthesizedArticle sa WHERE sa.title ILIKE %:query% OR sa.synthesizedNarrative ILIKE %:query%")
  Page<SynthesizedArticle> search(@Param("query") String query, Pageable pageable);

  List<SynthesizedArticle> findByRssFeedSource(String rssFeedSource);

  @Query("SELECT sa FROM SynthesizedArticle sa WHERE sa.sourceUrl = :sourceUrl")
  SynthesizedArticle findBySourceUrl(@Param("sourceUrl") String sourceUrl);

  long countByStatus(String status);

  long countByVerdict(String verdict);
}
