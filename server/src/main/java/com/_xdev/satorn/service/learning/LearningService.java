package com._xdev.satorn.service.learning;

import com.fasterxml.jackson.databind.JsonNode;
import com._xdev.satorn.ai.config.LLMFactory;
import com._xdev.satorn.ai.util.ResponseParser;
import com._xdev.satorn.domain.entity.LearnerProfile;
import com._xdev.satorn.domain.entity.LearningQuizQuestion;
import com._xdev.satorn.domain.entity.LearningQuizSession;
import com._xdev.satorn.domain.entity.LearningSkillProgress;
import com._xdev.satorn.domain.entity.SynthesizedArticle;
import com._xdev.satorn.domain.entity.User;
import com._xdev.satorn.domain.repository.LearnerProfileRepository;
import com._xdev.satorn.domain.repository.LearningQuizQuestionRepository;
import com._xdev.satorn.domain.repository.LearningQuizSessionRepository;
import com._xdev.satorn.domain.repository.LearningSkillProgressRepository;
import com._xdev.satorn.domain.repository.SynthesizedArticleRepository;
import com._xdev.satorn.domain.repository.UserRepository;
import com._xdev.satorn.dto.learning.LearningProfileRequest;
import com._xdev.satorn.dto.learning.QuizGenerateRequest;
import com._xdev.satorn.dto.learning.QuizSubmitRequest;
import com._xdev.satorn.dto.learning.TutorRequest;
import com._xdev.satorn.service.external.TavilySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningService {

  private static final Set<String> ALLOWED_DIFFICULTIES = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");
  private static final List<String> DEFAULT_CATEGORIES = List.of(
      "Politics",
      "Technology",
      "Economy",
      "Health",
      "Environment",
      "Security",
      "Media",
      "Society",
      "Science",
      "International");

  private final UserRepository userRepository;
  private final SynthesizedArticleRepository synthesizedArticleRepository;
  private final LearnerProfileRepository learnerProfileRepository;
  private final LearningQuizSessionRepository learningQuizSessionRepository;
  private final LearningQuizQuestionRepository learningQuizQuestionRepository;
  private final LearningSkillProgressRepository learningSkillProgressRepository;
  private final TavilySearchService tavilySearchService;
  private final LLMFactory llmFactory;

  @Transactional(readOnly = true)
  public Map<String, Object> getProfile(String username) {
    User user = resolveUser(username);
    LearnerProfile profile = learnerProfileRepository.findByUser(user)
        .orElseGet(() -> defaultProfile(user));

    return toProfileDto(profile);
  }

  @Transactional
  public Map<String, Object> upsertProfile(String username, LearningProfileRequest request) {
    User user = resolveUser(username);
    LearnerProfile profile = learnerProfileRepository.findByUser(user)
        .orElseGet(() -> defaultProfile(user));

    profile.setExamTrack(defaultIfBlank(request.getExamTrack(), profile.getExamTrack()));
    profile.setTargetExamDate(request.getTargetExamDate() != null ? request.getTargetExamDate() : profile.getTargetExamDate());
    profile.setDailyStudyMinutes(sanitizeStudyMinutes(request.getDailyStudyMinutes(), profile.getDailyStudyMinutes()));
    profile.setPreferredDifficulty(normalizeDifficulty(request.getPreferredDifficulty(), profile.getPreferredDifficulty()));
    profile.setWeakCategoriesCsv(joinCsv(normalizeCategoryList(request.getWeakCategories(), profile.getWeakCategoriesCsv())));
    profile.setStrongCategoriesCsv(joinCsv(normalizeCategoryList(request.getStrongCategories(), profile.getStrongCategoriesCsv())));
    profile.setLearningGoals(defaultIfBlank(request.getLearningGoals(), profile.getLearningGoals()));

    LearnerProfile saved = learnerProfileRepository.save(profile);
    return toProfileDto(saved);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getRecommendations(String username, int page, int size) {
    User user = resolveUser(username);
    LearnerProfile profile = learnerProfileRepository.findByUser(user).orElseGet(() -> defaultProfile(user));
    List<RecommendationCandidate> ranked = rankRecommendations(user, profile);

    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(size, 50));
    int total = ranked.size();
    int from = Math.min(safePage * safeSize, total);
    int to = Math.min(from + safeSize, total);

    List<Map<String, Object>> articles = ranked.subList(from, to).stream()
        .map(this::toRecommendationDto)
        .toList();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("total", total);
    response.put("page", safePage);
    response.put("size", safeSize);
    response.put("totalPages", safeSize == 0 ? 0 : (int) Math.ceil(total / (double) safeSize));
    response.put("articles", articles);
    return response;
  }

  @Transactional
  public Map<String, Object> generateQuiz(String username, QuizGenerateRequest request) {
    User user = resolveUser(username);
    LearnerProfile profile = learnerProfileRepository.findByUser(user).orElseGet(() -> defaultProfile(user));

    int questionCount = clampQuestionCount(request != null ? request.getQuestionCount() : null);
    String difficulty = normalizeDifficulty(request != null ? request.getDifficulty() : null, profile.getPreferredDifficulty());
    String focusCategory = request == null ? null : trimToNull(request.getCategory());

    LearningQuizSession session = LearningQuizSession.builder()
        .user(user)
        .focusCategory(focusCategory)
        .difficulty(difficulty)
        .status("GENERATED")
        .build();
    session = learningQuizSessionRepository.save(session);

    List<LearningQuizQuestion> generated = new ArrayList<>();
    List<Map<String, Object>> contextSources = new ArrayList<>();
    String generationStrategy = "TAVILY_NEWS_AI";
    String sourceQuery = null;

    TavilyQuizContext tavilyContext = fetchTavilyQuizContext(profile, focusCategory, questionCount);
    sourceQuery = tavilyContext.query();

    if (!tavilyContext.results().isEmpty()) {
      generated.addAll(generateQuizFromTavilyContext(
          session,
          tavilyContext.results(),
          questionCount,
          difficulty,
          focusCategory,
          profile));
      contextSources.addAll(toSourceDtos(tavilyContext.results()));
    }

    if (generated.isEmpty()) {
      generationStrategy = "INTERNAL_VERIFIED_FALLBACK";
      List<SynthesizedArticle> candidates = selectQuizCandidates(user, profile, focusCategory);
      if (candidates.isEmpty()) {
        throw new IllegalStateException("No current-affairs sources are available to generate quiz questions.");
      }

      List<String> categoryPool = extractCategoryPool(candidates);
      for (SynthesizedArticle article : candidates) {
        if (generated.size() >= questionCount) {
          break;
        }

        LearningQuizQuestion question = buildQuestionForArticle(
            session,
            article,
            generated.size() + 1,
            difficulty,
            categoryPool);

        if (question != null) {
          generated.add(question);
        }
      }
      contextSources.addAll(toArticleSourceDtos(candidates, Math.min(questionCount, 6)));
    }

    if (generated.isEmpty()) {
      throw new IllegalStateException("Unable to generate quiz from current-affairs inputs.");
    }

    session.setTotalQuestions(generated.size());
    session.getQuestions().clear();
    session.getQuestions().addAll(generated);
    learningQuizSessionRepository.save(session);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("quizSessionId", session.getId());
    response.put("status", session.getStatus());
    response.put("difficulty", session.getDifficulty());
    response.put("focusCategory", session.getFocusCategory());
    response.put("questionCount", generated.size());
    response.put("estimatedTimeMinutes", Math.max(5, generated.size() * 2));
    response.put("generationStrategy", generationStrategy);
    response.put("sourceQuery", sourceQuery);
    response.put("sources", contextSources);
    response.put("questions", generated.stream().map(this::toQuizQuestionDto).toList());
    return response;
  }

  @Transactional
  public Map<String, Object> submitQuiz(String username, QuizSubmitRequest request) {
    User user = resolveUser(username);
    LearningQuizSession session = learningQuizSessionRepository.findByIdAndUser(request.getQuizSessionId(), user)
        .orElseThrow(() -> new IllegalArgumentException("Quiz session not found"));

    List<LearningQuizQuestion> questions = learningQuizQuestionRepository.findBySessionOrderBySortOrderAsc(session);
    if (questions.isEmpty()) {
      throw new IllegalStateException("Quiz session does not contain questions");
    }

    Map<Long, String> answerMap = new HashMap<>();
    if (request.getAnswers() != null) {
      for (QuizSubmitRequest.AnswerSubmission answer : request.getAnswers()) {
        if (answer.getQuestionId() == null) {
          continue;
        }
        String option = normalizeOption(answer.getSelectedOption());
        if (option != null) {
          answerMap.put(answer.getQuestionId(), option);
        }
      }
    }

    int correct = 0;
    Map<String, int[]> categoryStats = new HashMap<>();
    List<Map<String, Object>> questionResults = new ArrayList<>();

    for (LearningQuizQuestion question : questions) {
      String selected = answerMap.get(question.getId());
      boolean isCorrect = selected != null && selected.equalsIgnoreCase(question.getCorrectOption());
      if (isCorrect) {
        correct++;
      }

      String category = normalizeCategoryName(question.getCategory());
      int[] stats = categoryStats.computeIfAbsent(category, ignored -> new int[] { 0, 0 });
      stats[0] += 1;
      if (isCorrect) {
        stats[1] += 1;
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("questionId", question.getId());
      result.put("question", question.getQuestionText());
      result.put("selectedOption", selected);
      result.put("correctOption", question.getCorrectOption());
      result.put("isCorrect", isCorrect);
      result.put("explanation", question.getExplanation());
      result.put("category", category);
      questionResults.add(result);
    }

    double score = round2((correct * 100.0) / questions.size());
    session.setStatus("SUBMITTED");
    session.setCorrectAnswers(correct);
    session.setTotalQuestions(questions.size());
    session.setScorePercent(score);
    learningQuizSessionRepository.save(session);

    List<Map<String, Object>> skillUpdates = updateSkillProgress(user, categoryStats);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("quizSessionId", session.getId());
    response.put("status", session.getStatus());
    response.put("scorePercent", score);
    response.put("correctAnswers", correct);
    response.put("totalQuestions", questions.size());
    response.put("skillsUpdated", skillUpdates);
    response.put("results", questionResults);
    return response;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getSkills(String username) {
    User user = resolveUser(username);
    List<LearningSkillProgress> skills = learningSkillProgressRepository.findByUserOrderByMasteryScoreAsc(user);

    List<Map<String, Object>> items = skills.stream()
        .map(this::toSkillDto)
        .toList();

    int totalAttempted = skills.stream().mapToInt(skill -> safeInt(skill.getAttemptedQuestions())).sum();
    int totalCorrect = skills.stream().mapToInt(skill -> safeInt(skill.getCorrectAnswers())).sum();
    double overallMastery = totalAttempted == 0 ? 0.0 : round2((totalCorrect * 100.0) / totalAttempted);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("overallMastery", overallMastery);
    response.put("totalAttemptedQuestions", totalAttempted);
    response.put("totalCorrectAnswers", totalCorrect);
    response.put("needsFocusCategory", skills.isEmpty() ? null : normalizeCategoryName(skills.get(0).getCategoryName()));
    response.put("strongestCategory", skills.isEmpty() ? null : normalizeCategoryName(skills.get(skills.size() - 1).getCategoryName()));
    response.put("skills", items);
    return response;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> askTutor(String username, TutorRequest request) {
    User user = resolveUser(username);
    LearnerProfile profile = learnerProfileRepository.findByUser(user).orElseGet(() -> defaultProfile(user));

    String question = trimToNull(request.getQuestion());
    if (question == null) {
      throw new IllegalArgumentException("Question cannot be empty");
    }

    List<RecommendationCandidate> recommendations = rankRecommendations(user, profile);
    List<SynthesizedArticle> contextArticles = new ArrayList<>(recommendations.stream()
        .limit(3)
        .map(RecommendationCandidate::article)
        .toList());

    if (request.getContextArticleId() != null) {
      synthesizedArticleRepository.findById(request.getContextArticleId())
          .ifPresent(contextArticle -> {
            if (contextArticles.stream().noneMatch(article -> Objects.equals(article.getId(), contextArticle.getId()))) {
              contextArticles.add(0, contextArticle);
            }
          });
    }

    while (contextArticles.size() > 3) {
      contextArticles.remove(contextArticles.size() - 1);
    }

    String tutorResponse = generateTutorResponse(question, profile, contextArticles);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("answer", tutorResponse);
    response.put("question", question);
    response.put("nextActions", List.of(
        "Attempt a quiz on your weakest category from /api/learning/quiz/generate",
        "Read one recommendation deeply and summarize it in 5 bullet points",
        "Revisit this tutor with a follow-up question on policy implications"));
    response.put("contextArticles", contextArticles.stream().map(this::toTutorContextDto).toList());
    return response;
  }

  private User resolveUser(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
  }

  private LearnerProfile defaultProfile(User user) {
    return LearnerProfile.builder()
        .user(user)
        .examTrack("General Current Affairs")
        .dailyStudyMinutes(45)
        .preferredDifficulty("INTERMEDIATE")
        .weakCategoriesCsv("")
        .strongCategoriesCsv("")
        .learningGoals("Improve current affairs retention and answer-writing quality")
        .build();
  }

  private Map<String, Object> toProfileDto(LearnerProfile profile) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("examTrack", defaultIfBlank(profile.getExamTrack(), "General Current Affairs"));
    dto.put("targetExamDate", profile.getTargetExamDate());
    dto.put("dailyStudyMinutes", sanitizeStudyMinutes(profile.getDailyStudyMinutes(), 45));
    dto.put("preferredDifficulty", normalizeDifficulty(profile.getPreferredDifficulty(), "INTERMEDIATE"));
    dto.put("weakCategories", splitCsv(profile.getWeakCategoriesCsv()));
    dto.put("strongCategories", splitCsv(profile.getStrongCategoriesCsv()));
    dto.put("learningGoals", defaultIfBlank(profile.getLearningGoals(), ""));
    dto.put("createdAt", profile.getCreatedAt());
    dto.put("updatedAt", profile.getUpdatedAt());
    return dto;
  }

  private List<RecommendationCandidate> rankRecommendations(User user, LearnerProfile profile) {
    List<SynthesizedArticle> candidates = loadRecommendationCandidates();
    Map<String, Double> masteryByCategory = loadMasteryByCategory(user);
    Set<String> weakCategories = new LinkedHashSet<>(splitCsv(profile.getWeakCategoriesCsv()));
    weakCategories.addAll(extractLowMasteryCategories(masteryByCategory, 60.0));
    Set<String> strongCategories = new LinkedHashSet<>(splitCsv(profile.getStrongCategoriesCsv()));
    Set<String> normalizedWeak = weakCategories.stream()
        .map(value -> value.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
    Set<String> normalizedStrong = strongCategories.stream()
        .map(value -> value.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());

    List<RecommendationCandidate> ranked = new ArrayList<>();
    for (SynthesizedArticle article : candidates) {
      double score = 0.0;
      List<String> reasons = new ArrayList<>();

      double credibility = normalizeCredibility(article.getCredibilityScore());
      score += credibility * 0.45;
      if (credibility >= 75) {
        reasons.add("High credibility verification");
      }

      double recency = recencyScore(article.getCreatedAt());
      score += recency * 0.30;
      if (recency > 70) {
        reasons.add("Recently verified");
      }

      if (Boolean.TRUE.equals(article.getIsTrending())) {
        score += 15;
        reasons.add("Trending among readers");
      }

      String category = normalizeCategoryName(article.getCategory() == null ? null : article.getCategory().getName());
      String normalizedCategory = category.toLowerCase(Locale.ROOT);

      if (normalizedWeak.contains(normalizedCategory)) {
        score += 18;
        reasons.add("Targets your weak area: " + category);
      } else if (normalizedStrong.contains(normalizedCategory)) {
        score += 8;
        reasons.add("Matches your preferred category: " + category);
      }

      Double mastery = masteryByCategory.get(normalizedCategory);
      if (mastery != null) {
        double boost = Math.max(0.0, 20.0 - (mastery / 5.0));
        score += boost;
        if (mastery < 60) {
          reasons.add("Useful for improving " + category + " mastery");
        }
      }

      if (reasons.isEmpty()) {
        reasons.add("Balanced pick based on credibility and freshness");
      }

      ranked.add(new RecommendationCandidate(article, round2(score), reasons));
    }

    ranked.sort((left, right) -> {
      int cmp = Double.compare(right.score(), left.score());
      if (cmp != 0) {
        return cmp;
      }
      LocalDateTime rightCreated = right.article().getCreatedAt();
      LocalDateTime leftCreated = left.article().getCreatedAt();
      if (rightCreated == null && leftCreated == null) {
        return 0;
      }
      if (rightCreated == null) {
        return -1;
      }
      if (leftCreated == null) {
        return 1;
      }
      return rightCreated.compareTo(leftCreated);
    });

    return ranked;
  }

  private List<SynthesizedArticle> loadRecommendationCandidates() {
    PageRequest pageRequest = PageRequest.of(0, 250, Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id")));

    Page<SynthesizedArticle> verified = synthesizedArticleRepository.findByStatus("VERIFIED", pageRequest);
    if (!verified.isEmpty()) {
      return verified.getContent();
    }
    return synthesizedArticleRepository.findAll(pageRequest).getContent();
  }

  private Map<String, Object> toRecommendationDto(RecommendationCandidate candidate) {
    SynthesizedArticle article = candidate.article();

    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("id", article.getId());
    dto.put("title", article.getTitle());
    dto.put("category", article.getCategory() != null ? article.getCategory().getName() : null);
    dto.put("verdict", article.getVerdict());
    dto.put("credibilityScore", article.getCredibilityScore());
    dto.put("createdAt", article.getCreatedAt());
    dto.put("sourceUrl", article.getSourceUrl());
    dto.put("score", candidate.score());
    dto.put("whyRecommended", candidate.reasons());
    return dto;
  }

  private int clampQuestionCount(Integer requested) {
    int defaultCount = 6;
    if (requested == null) {
      return defaultCount;
    }
    return Math.max(3, Math.min(requested, 15));
  }

  private List<SynthesizedArticle> selectQuizCandidates(User user, LearnerProfile profile, String focusCategory) {
    PageRequest pageRequest = PageRequest.of(0, 200, Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id")));

    List<SynthesizedArticle> source;
    if (focusCategory != null && !focusCategory.isBlank()) {
      source = synthesizedArticleRepository.findByCategory_NameIgnoreCase(focusCategory, pageRequest).getContent().stream()
          .filter(article -> "VERIFIED".equalsIgnoreCase(article.getStatus()))
          .toList();
    } else {
      source = synthesizedArticleRepository.findByStatus("VERIFIED", pageRequest).getContent();
    }

    if (source.isEmpty()) {
      source = synthesizedArticleRepository.findAll(pageRequest).getContent();
    }

    Map<String, Double> masteryByCategory = loadMasteryByCategory(user);
    Set<String> weakCategories = new LinkedHashSet<>(splitCsv(profile.getWeakCategoriesCsv()));
    weakCategories.addAll(extractLowMasteryCategories(masteryByCategory, 60.0));
    Set<String> normalizedWeak = weakCategories.stream()
        .map(category -> category.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());

    List<SynthesizedArticle> ranked = new ArrayList<>(source);
    ranked.sort((left, right) -> Double.compare(
        quizArticlePriority(right, masteryByCategory, normalizedWeak),
        quizArticlePriority(left, masteryByCategory, normalizedWeak)));

    return ranked;
  }

  private double quizArticlePriority(
      SynthesizedArticle article,
      Map<String, Double> masteryByCategory,
      Set<String> weakCategories) {
    double priority = normalizeCredibility(article.getCredibilityScore());
    priority += recencyScore(article.getCreatedAt()) * 0.6;

    String category = normalizeCategoryName(article.getCategory() == null ? null : article.getCategory().getName());
    String categoryKey = category.toLowerCase(Locale.ROOT);
    if (weakCategories.contains(categoryKey)) {
      priority += 20;
    }

    Double mastery = masteryByCategory.get(categoryKey);
    if (mastery != null) {
      priority += Math.max(0, 25 - (mastery / 4));
    }

    return priority;
  }

  private TavilyQuizContext fetchTavilyQuizContext(LearnerProfile profile, String focusCategory, int questionCount) {
    if (!tavilySearchService.isConfigured()) {
      return new TavilyQuizContext(null, List.of());
    }

    String quizTopic = resolveQuizTopic(profile, focusCategory);
    String query = buildQuizSearchQuery(quizTopic, profile.getExamTrack());

    TavilySearchService.SearchResults primary = tavilySearchService.advancedSearch(
        query,
        Math.max(10, questionCount + 4),
        "news");

    List<TavilySearchService.SearchResult> cleaned = sanitizeTavilyResults(primary, 12);
    if (cleaned.isEmpty()) {
      TavilySearchService.SearchResults fallback = tavilySearchService.searchRecentNews(
          "current affairs " + quizTopic,
          Math.max(10, questionCount + 4));
      cleaned = sanitizeTavilyResults(fallback, 12);
    }

    return new TavilyQuizContext(query, cleaned);
  }

  private String resolveQuizTopic(LearnerProfile profile, String focusCategory) {
    String explicit = trimToNull(focusCategory);
    if (explicit != null) {
      return normalizeCategoryName(explicit);
    }

    List<String> weakCategories = splitCsv(profile.getWeakCategoriesCsv());
    if (!weakCategories.isEmpty()) {
      return normalizeCategoryName(weakCategories.get(0));
    }

    String examTrack = trimToNull(profile.getExamTrack());
    if (examTrack != null) {
      return examTrack;
    }

    return "current affairs";
  }

  private String buildQuizSearchQuery(String topic, String examTrack) {
    String today = java.time.LocalDate.now().toString();
    String focusTrack = defaultIfBlank(trimToNull(examTrack), "civil services exam preparation");
    return "latest " + topic + " current affairs updates " + today
        + " with policy impact, governance implications, and factual data for " + focusTrack;
  }

  private List<TavilySearchService.SearchResult> sanitizeTavilyResults(
      TavilySearchService.SearchResults results,
      int limit) {
    if (results == null || results.getResults() == null || results.getResults().isEmpty()) {
      return List.of();
    }

    List<TavilySearchService.SearchResult> cleaned = new ArrayList<>();
    Set<String> seenUrls = new LinkedHashSet<>();
    for (TavilySearchService.SearchResult result : results.getResults()) {
      if (result == null) {
        continue;
      }
      String url = trimToNull(result.getUrl());
      String title = trimToNull(result.getTitle());
      String content = trimToNull(result.getContent());
      if (url == null || title == null || content == null || content.length() < 80) {
        continue;
      }
      if (!seenUrls.add(url)) {
        continue;
      }
      cleaned.add(result);
      if (cleaned.size() >= limit) {
        break;
      }
    }
    return cleaned;
  }

  private List<LearningQuizQuestion> generateQuizFromTavilyContext(
      LearningQuizSession session,
      List<TavilySearchService.SearchResult> tavilyResults,
      int questionCount,
      String fallbackDifficulty,
      String fallbackCategory,
      LearnerProfile profile) {
    List<AiQuizSpec> specs = generateAiQuizSpecs(
        tavilyResults,
        questionCount,
        fallbackDifficulty,
        fallbackCategory,
        profile);

    if (specs.isEmpty()) {
      return List.of();
    }

    List<LearningQuizQuestion> questions = new ArrayList<>();
    int order = 1;
    for (AiQuizSpec spec : specs) {
      LearningQuizQuestion question = toQuizEntityFromAiSpec(
          session,
          spec,
          order++,
          fallbackDifficulty,
          fallbackCategory);
      if (question != null) {
        questions.add(question);
      }
      if (questions.size() >= questionCount) {
        break;
      }
    }
    return questions;
  }

  private List<AiQuizSpec> generateAiQuizSpecs(
      List<TavilySearchService.SearchResult> tavilyResults,
      int questionCount,
      String difficulty,
      String focusCategory,
      LearnerProfile profile) {
    if (tavilyResults.isEmpty()) {
      return List.of();
    }

    String sourcesContext = buildTavilySourcesContext(tavilyResults);
    String examTrack = defaultIfBlank(trimToNull(profile.getExamTrack()), "civil services");
    String weakAreas = String.join(", ", splitCsv(profile.getWeakCategoriesCsv()));

    String prompt = """
        You are building a high-value current affairs quiz for competitive exam aspirants.

        Build exactly %d multiple-choice questions using ONLY the provided current-affairs sources.
        Difficulty: %s
        Preferred category/topic: %s
        Exam track: %s
        Weak areas: %s

        Quality rules:
        1) Questions must test analysis, policy implications, chronology, institutions, data interpretation, and governance relevance.
        2) Avoid trivial one-line recall.
        3) Ensure each question has 4 plausible options with one best answer.
        4) Add concise explanation and exam relevance.
        5) Tie each question to one source.

        Return STRICT JSON only:
        {
          "questions": [
            {
              "question": "string",
              "optionA": "string",
              "optionB": "string",
              "optionC": "string",
              "optionD": "string",
              "correctOption": "A|B|C|D",
              "explanation": "string",
              "examRelevance": "string",
              "category": "string",
              "difficulty": "BEGINNER|INTERMEDIATE|ADVANCED",
              "sourceTitle": "string",
              "sourceUrl": "https://..."
            }
          ]
        }

        Sources:
        %s
        """.formatted(
        questionCount,
        difficulty,
        defaultIfBlank(trimToNull(focusCategory), "Current Affairs"),
        examTrack,
        defaultIfBlank(trimToNull(weakAreas), "General"),
        sourcesContext);

    try {
      String raw = llmFactory.getClientForTask(LLMFactory.TaskType.CHAT)
          .prompt()
          .user(prompt)
          .call()
          .content();

      JsonNode json = ResponseParser.parseJsonNode(raw);
      JsonNode questionsNode = json.path("questions");
      if (!questionsNode.isArray() && json.isArray()) {
        questionsNode = json;
      }

      if (!questionsNode.isArray()) {
        return List.of();
      }

      List<AiQuizSpec> specs = new ArrayList<>();
      Set<String> dedupe = new LinkedHashSet<>();
      for (JsonNode node : questionsNode) {
        AiQuizSpec spec = parseAiQuizSpec(node);
        if (spec == null) {
          continue;
        }

        String signature = spec.question().toLowerCase(Locale.ROOT).trim();
        if (!dedupe.add(signature)) {
          continue;
        }

        specs.add(spec);
        if (specs.size() >= questionCount) {
          break;
        }
      }

      if (specs.size() < Math.min(3, questionCount)) {
        return List.of();
      }
      return specs;
    } catch (Exception e) {
      log.warn("AI quiz generation from Tavily context failed: {}", e.getMessage());
      return List.of();
    }
  }

  private String buildTavilySourcesContext(List<TavilySearchService.SearchResult> tavilyResults) {
    StringBuilder sb = new StringBuilder();
    int index = 1;
    for (TavilySearchService.SearchResult result : tavilyResults) {
      sb.append("Source ").append(index++).append(":\n");
      sb.append("Title: ").append(defaultIfBlank(result.getTitle(), "Untitled")).append("\n");
      sb.append("URL: ").append(defaultIfBlank(result.getUrl(), "N/A")).append("\n");
      sb.append("Snippet: ").append(truncate(defaultIfBlank(result.getContent(), ""), 650)).append("\n\n");
    }
    return sb.toString();
  }

  private AiQuizSpec parseAiQuizSpec(JsonNode node) {
    if (node == null || !node.isObject()) {
      return null;
    }

    String question = trimToNull(node.path("question").asText(null));
    String optionA = readQuestionOption(node, "A", "optionA");
    String optionB = readQuestionOption(node, "B", "optionB");
    String optionC = readQuestionOption(node, "C", "optionC");
    String optionD = readQuestionOption(node, "D", "optionD");
    String correctOption = normalizeOption(node.path("correctOption").asText(null));

    if (correctOption == null) {
      correctOption = normalizeOption(node.path("answer").asText(null));
    }

    String explanation = trimToNull(node.path("explanation").asText(null));
    String examRelevance = trimToNull(node.path("examRelevance").asText(null));
    String category = trimToNull(node.path("category").asText(null));
    String difficulty = trimToNull(node.path("difficulty").asText(null));
    String sourceTitle = trimToNull(node.path("sourceTitle").asText(null));
    String sourceUrl = trimToNull(node.path("sourceUrl").asText(null));

    AiQuizSpec spec = new AiQuizSpec(
        question,
        optionA,
        optionB,
        optionC,
        optionD,
        correctOption,
        explanation,
        examRelevance,
        category,
        difficulty,
        sourceTitle,
        sourceUrl);

    return isValidAiQuizSpec(spec) ? spec : null;
  }

  private String readQuestionOption(JsonNode node, String key, String flatKey) {
    String value = trimToNull(node.path(flatKey).asText(null));
    if (value != null) {
      return value;
    }

    JsonNode optionsNode = node.path("options");
    if (optionsNode.isObject()) {
      value = trimToNull(optionsNode.path(key).asText(null));
      if (value == null) {
        value = trimToNull(optionsNode.path(key.toLowerCase(Locale.ROOT)).asText(null));
      }
    }
    return value;
  }

  private boolean isValidAiQuizSpec(AiQuizSpec spec) {
    if (spec == null) {
      return false;
    }
    if (spec.question() == null || spec.question().length() < 20) {
      return false;
    }

    if (spec.correctOption() == null || !List.of("A", "B", "C", "D").contains(spec.correctOption())) {
      return false;
    }

    List<String> options = List.of(spec.optionA(), spec.optionB(), spec.optionC(), spec.optionD());
    if (options.stream().anyMatch(option -> option == null || option.isBlank())) {
      return false;
    }

    long distinct = options.stream()
        .map(option -> option.toLowerCase(Locale.ROOT).trim())
        .distinct()
        .count();

    return distinct == 4;
  }

  private LearningQuizQuestion toQuizEntityFromAiSpec(
      LearningQuizSession session,
      AiQuizSpec spec,
      int order,
      String fallbackDifficulty,
      String fallbackCategory) {
    if (!isValidAiQuizSpec(spec)) {
      return null;
    }

    String category = normalizeCategoryName(firstNonBlank(spec.category(), fallbackCategory, "Current Affairs"));
    String difficulty = normalizeDifficulty(spec.difficulty(), fallbackDifficulty);
    String explanation = composeAiExplanation(spec);

    return LearningQuizQuestion.builder()
        .session(session)
        .synthesizedArticle(null)
        .category(category)
        .difficulty(difficulty)
        .questionText(spec.question().trim())
        .optionA(spec.optionA().trim())
        .optionB(spec.optionB().trim())
        .optionC(spec.optionC().trim())
        .optionD(spec.optionD().trim())
        .correctOption(spec.correctOption())
        .explanation(explanation)
        .sortOrder(order)
        .build();
  }

  private String composeAiExplanation(AiQuizSpec spec) {
    StringBuilder explanation = new StringBuilder();
    if (spec.explanation() != null) {
      explanation.append(spec.explanation().trim());
    }
    if (spec.examRelevance() != null && !spec.examRelevance().isBlank()) {
      if (explanation.length() > 0) {
        explanation.append("\n\n");
      }
      explanation.append("Exam relevance: ").append(spec.examRelevance().trim());
    }
    if (spec.sourceTitle() != null || spec.sourceUrl() != null) {
      if (explanation.length() > 0) {
        explanation.append("\n\n");
      }
      explanation.append("Source: ");
      explanation.append(defaultIfBlank(spec.sourceTitle(), "Current affairs source"));
      if (spec.sourceUrl() != null) {
        explanation.append(" (").append(spec.sourceUrl()).append(")");
      }
    }
    return explanation.toString();
  }

  private List<Map<String, Object>> toSourceDtos(List<TavilySearchService.SearchResult> results) {
    List<Map<String, Object>> sources = new ArrayList<>();
    for (TavilySearchService.SearchResult result : results) {
      Map<String, Object> source = new LinkedHashMap<>();
      source.put("title", result.getTitle());
      source.put("url", result.getUrl());
      source.put("score", result.getScore());
      source.put("snippet", truncate(defaultIfBlank(result.getContent(), ""), 220));
      sources.add(source);
    }
    return sources;
  }

  private List<Map<String, Object>> toArticleSourceDtos(List<SynthesizedArticle> articles, int limit) {
    List<Map<String, Object>> sources = new ArrayList<>();
    int count = 0;
    for (SynthesizedArticle article : articles) {
      if (count >= limit) {
        break;
      }
      Map<String, Object> source = new LinkedHashMap<>();
      source.put("title", article.getTitle());
      source.put("url", article.getSourceUrl());
      source.put("score", article.getCredibilityScore());
      source.put("snippet", truncate(defaultIfBlank(article.getSynthesizedNarrative(), article.getKeyFindings()), 220));
      sources.add(source);
      count++;
    }
    return sources;
  }

  private LearningQuizQuestion buildQuestionForArticle(
      LearningQuizSession session,
      SynthesizedArticle article,
      int order,
      String difficulty,
      List<String> categoryPool) {

    int type = order % 3;
    return switch (type) {
      case 1 -> buildVerdictQuestion(session, article, order, difficulty);
      case 2 -> buildCredibilityQuestion(session, article, order, difficulty);
      default -> buildCategoryQuestion(session, article, order, difficulty, categoryPool);
    };
  }

  private LearningQuizQuestion buildVerdictQuestion(
      LearningQuizSession session,
      SynthesizedArticle article,
      int order,
      String difficulty) {
    String title = defaultIfBlank(article.getTitle(), "this article");
    String normalized = normalizeVerdictBand(article.getVerdict());

    Map<String, String> optionMap = new LinkedHashMap<>();
    optionMap.put("A", "Mostly true");
    optionMap.put("B", "Mixed / partially verified");
    optionMap.put("C", "Mostly false");
    optionMap.put("D", "Unverifiable right now");

    String correct = switch (normalized) {
      case "MOSTLY_TRUE" -> "A";
      case "MOSTLY_FALSE" -> "C";
      case "UNVERIFIABLE" -> "D";
      default -> "B";
    };

    return LearningQuizQuestion.builder()
        .session(session)
        .synthesizedArticle(article)
        .category(normalizeCategoryName(article.getCategory() == null ? null : article.getCategory().getName()))
        .difficulty(difficulty)
        .questionText("What is the closest verification verdict for: \"" + title + "\"?")
        .optionA(optionMap.get("A"))
        .optionB(optionMap.get("B"))
        .optionC(optionMap.get("C"))
        .optionD(optionMap.get("D"))
        .correctOption(correct)
        .explanation("Stored verdict for this article is " + defaultIfBlank(article.getVerdict(), "MIXED")
            + " with credibility score " + normalizeCredibility(article.getCredibilityScore()) + ".")
        .sortOrder(order)
        .build();
  }

  private LearningQuizQuestion buildCredibilityQuestion(
      LearningQuizSession session,
      SynthesizedArticle article,
      int order,
      String difficulty) {
    double score = normalizeCredibility(article.getCredibilityScore());
    String title = defaultIfBlank(article.getTitle(), "this article");

    Map<String, String> optionMap = new LinkedHashMap<>();
    optionMap.put("A", "High confidence (75-100)");
    optionMap.put("B", "Moderate confidence (50-74)");
    optionMap.put("C", "Low confidence (25-49)");
    optionMap.put("D", "Very low confidence (0-24)");

    String correct;
    if (score >= 75) {
      correct = "A";
    } else if (score >= 50) {
      correct = "B";
    } else if (score >= 25) {
      correct = "C";
    } else {
      correct = "D";
    }

    return LearningQuizQuestion.builder()
        .session(session)
        .synthesizedArticle(article)
        .category(normalizeCategoryName(article.getCategory() == null ? null : article.getCategory().getName()))
        .difficulty(difficulty)
        .questionText("Which confidence band best matches the verification score of \"" + title + "\"?")
        .optionA(optionMap.get("A"))
        .optionB(optionMap.get("B"))
        .optionC(optionMap.get("C"))
        .optionD(optionMap.get("D"))
        .correctOption(correct)
        .explanation("The article has credibility score " + score + ".")
        .sortOrder(order)
        .build();
  }

  private LearningQuizQuestion buildCategoryQuestion(
      LearningQuizSession session,
      SynthesizedArticle article,
      int order,
      String difficulty,
      List<String> categoryPool) {
    String title = defaultIfBlank(article.getTitle(), "this article");
    String correctCategory = normalizeCategoryName(article.getCategory() == null ? null : article.getCategory().getName());

    List<String> distractors = categoryPool.stream()
        .filter(category -> !category.equalsIgnoreCase(correctCategory))
        .distinct()
        .limit(3)
        .collect(Collectors.toCollection(ArrayList::new));

    if (distractors.size() < 3) {
      for (String category : DEFAULT_CATEGORIES) {
        if (distractors.size() >= 3) {
          break;
        }
        if (!category.equalsIgnoreCase(correctCategory) && distractors.stream().noneMatch(category::equalsIgnoreCase)) {
          distractors.add(category);
        }
      }
    }

    if (distractors.size() < 3) {
      return null;
    }

    List<String> options = new ArrayList<>();
    options.add(correctCategory);
    options.addAll(distractors.subList(0, 3));

    int rotateBy = article.getId() == null ? 0 : (int) (article.getId() % 4);
    Collections.rotate(options, rotateBy);

    int correctIndex = options.indexOf(correctCategory);
    String correctOption = toOptionLetter(correctIndex);

    return LearningQuizQuestion.builder()
        .session(session)
        .synthesizedArticle(article)
        .category(correctCategory)
        .difficulty(difficulty)
        .questionText("Which category best fits the verified article: \"" + title + "\"?")
        .optionA(options.get(0))
        .optionB(options.get(1))
        .optionC(options.get(2))
        .optionD(options.get(3))
        .correctOption(correctOption)
        .explanation("The article is currently tagged under " + correctCategory + ".")
        .sortOrder(order)
        .build();
  }

  private Map<String, Object> toQuizQuestionDto(LearningQuizQuestion question) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("id", question.getId());
    dto.put("question", question.getQuestionText());
    dto.put("category", normalizeCategoryName(question.getCategory()));
    dto.put("difficulty", normalizeDifficulty(question.getDifficulty(), "INTERMEDIATE"));
    dto.put("sourceArticleId", question.getSynthesizedArticle() != null ? question.getSynthesizedArticle().getId() : null);

    Map<String, String> options = new LinkedHashMap<>();
    options.put("A", question.getOptionA());
    options.put("B", question.getOptionB());
    options.put("C", question.getOptionC());
    options.put("D", question.getOptionD());
    dto.put("options", options);
    return dto;
  }

  private List<Map<String, Object>> updateSkillProgress(User user, Map<String, int[]> categoryStats) {
    List<LearningSkillProgress> toSave = new ArrayList<>();
    List<Map<String, Object>> response = new ArrayList<>();

    for (Map.Entry<String, int[]> entry : categoryStats.entrySet()) {
      String category = entry.getKey();
      int attempted = entry.getValue()[0];
      int correct = entry.getValue()[1];

      LearningSkillProgress skill = learningSkillProgressRepository.findByUserAndCategoryNameIgnoreCase(user, category)
          .orElseGet(() -> LearningSkillProgress.builder()
              .user(user)
              .categoryName(category)
              .masteryScore(0.0)
              .attemptedQuestions(0)
              .correctAnswers(0)
              .build());

      skill.setAttemptedQuestions(safeInt(skill.getAttemptedQuestions()) + attempted);
      skill.setCorrectAnswers(safeInt(skill.getCorrectAnswers()) + correct);
      skill.setMasteryScore(round2((skill.getCorrectAnswers() * 100.0) / Math.max(1, skill.getAttemptedQuestions())));
      skill.setLastPracticedAt(LocalDateTime.now());
      toSave.add(skill);
    }

    learningSkillProgressRepository.saveAll(toSave);

    for (LearningSkillProgress skill : toSave) {
      response.add(toSkillDto(skill));
    }

    return response;
  }

  private Map<String, Object> toSkillDto(LearningSkillProgress skill) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("category", normalizeCategoryName(skill.getCategoryName()));
    dto.put("masteryScore", round2(skill.getMasteryScore()));
    dto.put("attemptedQuestions", safeInt(skill.getAttemptedQuestions()));
    dto.put("correctAnswers", safeInt(skill.getCorrectAnswers()));
    dto.put("accuracyPercent", safeInt(skill.getAttemptedQuestions()) == 0
        ? 0.0
        : round2((safeInt(skill.getCorrectAnswers()) * 100.0) / safeInt(skill.getAttemptedQuestions())));
    dto.put("lastPracticedAt", skill.getLastPracticedAt());
    return dto;
  }

  private String generateTutorResponse(String question, LearnerProfile profile, List<SynthesizedArticle> articles) {
    String profileContext = """
        Exam track: %s
        Preferred difficulty: %s
        Daily study minutes: %s
        Weak categories: %s
        Strong categories: %s
        Learning goals: %s
        """.formatted(
        defaultIfBlank(profile.getExamTrack(), "General Current Affairs"),
        normalizeDifficulty(profile.getPreferredDifficulty(), "INTERMEDIATE"),
        sanitizeStudyMinutes(profile.getDailyStudyMinutes(), 45),
        String.join(", ", splitCsv(profile.getWeakCategoriesCsv())),
        String.join(", ", splitCsv(profile.getStrongCategoriesCsv())),
        defaultIfBlank(profile.getLearningGoals(), "Improve exam readiness"));

    String articleContext = articles.stream()
        .map(article -> """
            Title: %s
            Category: %s
            Verdict: %s
            Credibility score: %s
            Key findings: %s
            """.formatted(
            defaultIfBlank(article.getTitle(), "Untitled"),
            normalizeCategoryName(article.getCategory() == null ? null : article.getCategory().getName()),
            defaultIfBlank(article.getVerdict(), "MIXED"),
            normalizeCredibility(article.getCredibilityScore()),
            truncate(defaultIfBlank(article.getKeyFindings(), article.getSynthesizedNarrative()), 450)))
        .collect(Collectors.joining("\n"));

    if (articleContext.isBlank()) {
      articleContext = "No contextual articles were available.";
    }

    String prompt = """
        You are an AI current-affairs tutor for competitive exam preparation.
        Teach clearly and make the answer actionable for revision.

        Student profile:
        %s

        Trusted context from verified articles:
        %s

        Student question:
        %s

        Response rules:
        1) Start with a direct explanation in simple language.
        2) Add an "Exam Angle" section (how this may appear in exams/interviews).
        3) Add a "Remember This" section with 3 short bullets.
        4) Add 2 quick practice questions.
        Keep it concise and focused.
        """.formatted(profileContext, articleContext, question);

    try {
      return llmFactory.getClientForTask(LLMFactory.TaskType.CHAT)
          .prompt()
          .user(prompt)
          .call()
          .content();
    } catch (Exception e) {
      log.warn("Tutor model unavailable, using fallback response: {}", e.getMessage());
      return buildTutorFallback(question, articles);
    }
  }

  private String buildTutorFallback(String question, List<SynthesizedArticle> articles) {
    StringBuilder fallback = new StringBuilder();
    fallback.append("Direct Answer:\n");
    fallback.append("I could not reach the tutor model right now, so here is a verified-context answer.\n\n");
    fallback.append("Your question: ").append(question).append("\n\n");
    fallback.append("Exam Angle:\n");
    fallback.append("- Focus on cause, impact, and policy relevance.\n");
    fallback.append("- Compare at least two verified viewpoints in your answer-writing.\n\n");
    fallback.append("Remember This:\n");
    int count = 0;
    for (SynthesizedArticle article : articles) {
      if (count >= 3) {
        break;
      }
      fallback.append("- ").append(defaultIfBlank(article.getTitle(), "Untitled"))
          .append(" (").append(defaultIfBlank(article.getVerdict(), "MIXED")).append(")\n");
      count++;
    }
    if (count == 0) {
      fallback.append("- Re-run this question after at least one verified article is available.\n");
    }
    fallback.append("\nQuick Practice Questions:\n");
    fallback.append("1. What are the main stakeholders and how are they affected?\n");
    fallback.append("2. Which claim in this topic needs stronger evidence and why?\n");
    return fallback.toString();
  }

  private Map<String, Object> toTutorContextDto(SynthesizedArticle article) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("id", article.getId());
    dto.put("title", article.getTitle());
    dto.put("category", article.getCategory() != null ? article.getCategory().getName() : null);
    dto.put("verdict", article.getVerdict());
    dto.put("credibilityScore", article.getCredibilityScore());
    dto.put("sourceUrl", article.getSourceUrl());
    return dto;
  }

  private Map<String, Double> loadMasteryByCategory(User user) {
    return learningSkillProgressRepository.findByUserOrderByMasteryScoreAsc(user).stream()
        .collect(Collectors.toMap(
            skill -> normalizeCategoryName(skill.getCategoryName()).toLowerCase(Locale.ROOT),
            skill -> round2(skill.getMasteryScore()),
            (left, right) -> right,
            LinkedHashMap::new));
  }

  private Set<String> extractLowMasteryCategories(Map<String, Double> masteryByCategory, double threshold) {
    return masteryByCategory.entrySet().stream()
        .filter(entry -> entry.getValue() < threshold)
        .map(Map.Entry::getKey)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private List<String> extractCategoryPool(List<SynthesizedArticle> articles) {
    List<String> pool = articles.stream()
        .map(article -> normalizeCategoryName(article.getCategory() == null ? null : article.getCategory().getName()))
        .filter(name -> !name.isBlank())
        .distinct()
        .collect(Collectors.toCollection(ArrayList::new));
    if (pool.size() < 4) {
      for (String defaultCategory : DEFAULT_CATEGORIES) {
        if (pool.size() >= 8) {
          break;
        }
        if (pool.stream().noneMatch(defaultCategory::equalsIgnoreCase)) {
          pool.add(defaultCategory);
        }
      }
    }
    return pool;
  }

  private String toOptionLetter(int index) {
    return switch (index) {
      case 0 -> "A";
      case 1 -> "B";
      case 2 -> "C";
      default -> "D";
    };
  }

  private String normalizeOption(String option) {
    if (option == null || option.isBlank()) {
      return null;
    }
    String normalized = option.trim().toUpperCase(Locale.ROOT);
    if (List.of("A", "B", "C", "D").contains(normalized)) {
      return normalized;
    }

    for (String allowed : List.of("A", "B", "C", "D")) {
      if (normalized.startsWith(allowed)) {
        return allowed;
      }
    }
    return null;
  }

  private List<String> normalizeCategoryList(List<String> input, String fallbackCsv) {
    List<String> source = input == null ? splitCsv(fallbackCsv) : input;
    return source.stream()
        .map(this::trimToNull)
        .filter(Objects::nonNull)
        .map(this::normalizeCategoryName)
        .distinct()
        .limit(12)
        .toList();
  }

  private String joinCsv(List<String> values) {
    if (values == null || values.isEmpty()) {
      return "";
    }
    return String.join(",", values);
  }

  private List<String> splitCsv(String csv) {
    if (csv == null || csv.isBlank()) {
      return new ArrayList<>();
    }
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .distinct()
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private String normalizeDifficulty(String value, String fallback) {
    String candidate = value == null || value.isBlank() ? fallback : value;
    String normalized = candidate == null ? "INTERMEDIATE" : candidate.trim().toUpperCase(Locale.ROOT);
    if (!ALLOWED_DIFFICULTIES.contains(normalized)) {
      return "INTERMEDIATE";
    }
    return normalized;
  }

  private String normalizeCategoryName(String value) {
    if (value == null || value.isBlank()) {
      return "General";
    }
    String trimmed = value.trim();
    if (trimmed.length() == 1) {
      return trimmed.toUpperCase(Locale.ROOT);
    }
    return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
  }

  private String normalizeVerdictBand(String verdict) {
    String normalized = verdict == null ? "" : verdict.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "TRUE", "VERIFIED", "MOSTLY_TRUE", "PARTIALLY_TRUE", "PARTIALLY_VERIFIED" -> "MOSTLY_TRUE";
      case "FALSE", "MOSTLY_FALSE", "CONTRADICTED" -> "MOSTLY_FALSE";
      case "UNVERIFIABLE" -> "UNVERIFIABLE";
      default -> "MIXED";
    };
  }

  private double normalizeCredibility(Double value) {
    if (value == null) {
      return 0.0;
    }
    double score = value;
    if (score <= 1.0) {
      score = score * 100.0;
    }
    return round2(Math.max(0.0, Math.min(100.0, score)));
  }

  private double recencyScore(LocalDateTime createdAt) {
    if (createdAt == null) {
      return 0.0;
    }
    long hours = Math.max(0, Duration.between(createdAt, LocalDateTime.now()).toHours());
    double score = 100.0 - Math.min(100.0, hours / 2.0);
    return round2(Math.max(0.0, score));
  }

  private int sanitizeStudyMinutes(Integer requested, Integer fallback) {
    int base = requested == null ? (fallback == null ? 45 : fallback) : requested;
    return Math.max(10, Math.min(base, 360));
  }

  private String defaultIfBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private int safeInt(Integer value) {
    return value == null ? 0 : value;
  }

  private double round2(Double value) {
    if (value == null) {
      return 0.0;
    }
    return Math.round(value * 100.0) / 100.0;
  }

  private double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
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

  private record TavilyQuizContext(String query, List<TavilySearchService.SearchResult> results) {
  }

  private record AiQuizSpec(
      String question,
      String optionA,
      String optionB,
      String optionC,
      String optionD,
      String correctOption,
      String explanation,
      String examRelevance,
      String category,
      String difficulty,
      String sourceTitle,
      String sourceUrl) {
  }

  private record RecommendationCandidate(SynthesizedArticle article, double score, List<String> reasons) {
  }
}
