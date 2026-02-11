package com._xdev.satorn.service;

import com._xdev.satorn.dto.ArticleRequest;
import com._xdev.satorn.dto.ArticleResponse;
import com._xdev.satorn.domain.entity.Article;
import com._xdev.satorn.domain.entity.User;
import com._xdev.satorn.domain.repository.ArticleRepository;
import com._xdev.satorn.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;

  @Transactional
  public Article submitArticle(ArticleRequest request, String username) {
    log.info("Article submission from user: {}", username);

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

    Article article = Article.builder()
        .title(request.getTitle())
        .content(request.getContent())
        .url(request.getUrl())
        .author(request.getAuthor())
        .status("PENDING")
        .submittedBy(user)
        .submittedAt(LocalDateTime.now())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    return articleRepository.save(article);
  }

  @Transactional(readOnly = true)
  public Article getArticleById(Long id) {
    return articleRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Article not found: " + id));
  }

  @Transactional(readOnly = true)
  public Page<ArticleResponse> getAllArticles(Pageable pageable) {
    return articleRepository.findAll(pageable)
        .map(this::mapToResponse);
  }

  @Transactional(readOnly = true)
  public Page<ArticleResponse> getArticlesByUser(String username, Pageable pageable) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

    return articleRepository.findBySubmittedBy(user, pageable)
        .map(this::mapToResponse);
  }

  @Transactional(readOnly = true)
  public boolean isArticleOwner(Long articleId, String username) {
    return articleRepository.findById(articleId)
        .map(article -> article.getSubmittedBy() != null && article.getSubmittedBy().getUsername().equals(username))
        .orElse(false);
  }

  @Transactional
  public void reprocessArticle(Long id) {
    Article article = getArticleById(id);
    article.setStatus("PENDING");
    article.setUpdatedAt(LocalDateTime.now());
    articleRepository.save(article);
    log.info("Article reprocessed: {}", id);
  }

  public void deleteArticle(Long id) {
    articleRepository.deleteById(id);
  }

  public ArticleResponse mapToResponse(Article article) {
    return ArticleResponse.builder()
        .id(article.getId())
        .url(article.getUrl())
        .title(article.getTitle())
        .status(article.getStatus())
        .credibilityScore(article.getCredibilityScore())
        .submittedBy(article.getSubmittedBy() != null ? article.getSubmittedBy().getUsername() : null)
        .submittedAt(article.getSubmittedAt())
        .verifiedAt(article.getVerifiedAt())
        .build();
  }
}
