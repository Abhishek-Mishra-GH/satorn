package com._xdev.satorn.service.ai;

import com._xdev.satorn.domain.entity.SynthesizedArticle;
import com._xdev.satorn.domain.repository.SynthesizedArticleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class RagContextService {

  private final VectorStore vectorStore;
  private final SynthesizedArticleRepository synthesizedArticleRepository;

  public RagContextService(
      ObjectProvider<VectorStore> vectorStoreProvider,
      SynthesizedArticleRepository synthesizedArticleRepository) {
    this.vectorStore = vectorStoreProvider.getIfAvailable();
    this.synthesizedArticleRepository = synthesizedArticleRepository;
  }

  public String buildContext(String query, int maxItems) {
    List<String> snippets = new ArrayList<>();
    snippets.addAll(fromVectorStore(query, maxItems));

    if (snippets.isEmpty()) {
      snippets.addAll(fromSynthesizedFallback(query, maxItems));
    }

    if (snippets.isEmpty()) {
      return "";
    }
    return String.join("\n\n", snippets);
  }

  public void indexSynthesizedArticle(SynthesizedArticle article) {
    if (vectorStore == null || article == null) {
      return;
    }

    try {
      String content = String.join("\n\n",
          safe(article.getTitle()),
          safe(article.getSynthesizedNarrative()),
          safe(article.getKeyFindings()));

      if (content.isBlank()) {
        return;
      }

      Map<String, Object> metadata = new HashMap<>();
      metadata.put("type", "synthesized_article");
      metadata.put("articleId", article.getId());
      metadata.put("sourceUrl", safe(article.getSourceUrl()));
      metadata.put("verdict", safe(article.getVerdict()));
      metadata.put("credibilityScore", article.getCredibilityScore());

      vectorStore.add(List.of(new Document(content, metadata)));
    } catch (Exception e) {
      log.warn("Failed to index synthesized article in vector store: {}", article.getId(), e);
    }
  }

  private List<String> fromVectorStore(String query, int maxItems) {
    if (vectorStore == null || query == null || query.isBlank()) {
      return List.of();
    }

    try {
      SearchRequest request = SearchRequest.query(query)
          .withTopK(Math.max(1, maxItems))
          .withSimilarityThresholdAll();

      List<Document> docs = vectorStore.similaritySearch(request);
      List<String> snippets = new ArrayList<>();

      for (Document doc : docs) {
        String content = safe(doc.getContent());
        if (!content.isBlank()) {
          snippets.add(truncate(content, 600));
        }
      }
      return snippets;
    } catch (Exception e) {
      log.debug("Vector retrieval unavailable, using fallback", e);
      return List.of();
    }
  }

  private List<String> fromSynthesizedFallback(String query, int maxItems) {
    try {
      var page = synthesizedArticleRepository.search(query, PageRequest.of(0, Math.max(1, maxItems)));
      List<String> snippets = new ArrayList<>();
      for (SynthesizedArticle article : page.getContent()) {
        String snippet = "Title: " + safe(article.getTitle()) + "\n" +
            "Verdict: " + safe(article.getVerdict()) + " | Credibility: " + safe(article.getCredibilityScore()) +
            "\nSummary: " + truncate(safe(article.getSynthesizedNarrative()), 450);
        snippets.add(snippet);
      }
      return snippets;
    } catch (Exception e) {
      log.debug("Fallback synthesized article retrieval failed", e);
      return List.of();
    }
  }

  private String truncate(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    if (value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength) + "...";
  }

  private String safe(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
