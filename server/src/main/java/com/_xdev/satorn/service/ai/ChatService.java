package com._xdev.satorn.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com._xdev.satorn.ai.config.LLMFactory;
import com._xdev.satorn.ai.util.ResponseParser;
import com._xdev.satorn.domain.entity.*;
import com._xdev.satorn.domain.repository.*;
import com._xdev.satorn.service.external.ArticleScrapingService;
import com._xdev.satorn.service.external.TavilySearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates user chat with history, RAG, Tavily news search,
 * and URL-based verification workflow.
 */
@Slf4j
@Service
public class ChatService {

  private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s]+)", Pattern.CASE_INSENSITIVE);
  private static final int MAX_LINK_VERIFICATION_CLAIMS = 5;
  private static final int NEWS_SEARCH_RESULTS = 5;
  private static final Set<String> ALLOWED_INTENTS = Set.of(
      "LINK_VERIFICATION",
      "CURRENT_NEWS",
      "HOW_IT_WORKS",
      "HELP",
      "GENERAL_CHAT");

  private final LLMFactory llmFactory;
  private final RagContextService ragContextService;
  private final TavilySearchService tavilySearchService;
  private final ArticleScrapingService articleScrapingService;
  private final ClaimExtractionService claimExtractionService;
  private final VerificationService verificationService;
  private final SynthesisService synthesisService;
  private final CategoryTaggingService categoryTaggingService;

  private final ChatSessionRepository chatSessionRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final UserRepository userRepository;
  private final ArticleRepository articleRepository;
  private final ClaimRepository claimRepository;
  private final VerificationRepository verificationRepository;
  private final SynthesisRepository synthesisRepository;
  private final CategoryRepository categoryRepository;
  private final SynthesizedArticleRepository synthesizedArticleRepository;

  public ChatService(
      LLMFactory llmFactory,
      RagContextService ragContextService,
      TavilySearchService tavilySearchService,
      ArticleScrapingService articleScrapingService,
      ClaimExtractionService claimExtractionService,
      VerificationService verificationService,
      SynthesisService synthesisService,
      CategoryTaggingService categoryTaggingService,
      ChatSessionRepository chatSessionRepository,
      ChatMessageRepository chatMessageRepository,
      UserRepository userRepository,
      ArticleRepository articleRepository,
      ClaimRepository claimRepository,
      VerificationRepository verificationRepository,
      SynthesisRepository synthesisRepository,
      CategoryRepository categoryRepository,
      SynthesizedArticleRepository synthesizedArticleRepository) {
    this.llmFactory = llmFactory;
    this.ragContextService = ragContextService;
    this.tavilySearchService = tavilySearchService;
    this.articleScrapingService = articleScrapingService;
    this.claimExtractionService = claimExtractionService;
    this.verificationService = verificationService;
    this.synthesisService = synthesisService;
    this.categoryTaggingService = categoryTaggingService;
    this.chatSessionRepository = chatSessionRepository;
    this.chatMessageRepository = chatMessageRepository;
    this.userRepository = userRepository;
    this.articleRepository = articleRepository;
    this.claimRepository = claimRepository;
    this.verificationRepository = verificationRepository;
    this.synthesisRepository = synthesisRepository;
    this.categoryRepository = categoryRepository;
    this.synthesizedArticleRepository = synthesizedArticleRepository;
  }

  @Transactional
  public ChatResponse processMessage(Long sessionId, String userMessage, String username) {
    try {
      ChatSession session = getOrCreateSession(sessionId, username, userMessage);
      String intent = detectIntent(userMessage);

      ChatMessage userMsg = saveMessage(session, "user", userMessage, intent);
      String aiResponse = generateAssistantResponse(session, userMessage, intent, null);
      saveMessage(session, "assistant", aiResponse, intent);

      if (!isGuestUser(username)) {
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);
      }

      return new ChatResponse(session.getId(), userMsg.getContent(), aiResponse, intent);
    } catch (Exception e) {
      log.error("Failed to process chat message for session: {}", sessionId, e);
      return new ChatResponse(sessionId, userMessage, "Error processing your message", "ERROR");
    }
  }

  public SseEmitter streamMessage(Long sessionId, String userMessage, String username) {
    SseEmitter emitter = new SseEmitter(0L);

    CompletableFuture.runAsync(() -> {
      try {
        ChatSession session = getOrCreateSession(sessionId, username, userMessage);
        String intent = detectIntent(userMessage);

        saveMessage(session, "user", userMessage, intent);
        emitProgress(emitter, "thinking", "Understanding your request", 5);

        String response = generateAssistantResponse(session, userMessage, intent,
            (stage, message, percent) -> emitProgress(emitter, stage, message, percent));

        saveMessage(session, "assistant", response, intent);
        if (!isGuestUser(username)) {
          session.setUpdatedAt(LocalDateTime.now());
          chatSessionRepository.save(session);
        }

        streamTextResponse(emitter, response);
        Map<String, Object> completedPayload = new HashMap<>();
        completedPayload.put("sessionId", session.getId());
        completedPayload.put("intent", defaultIfBlank(intent, ""));
        completedPayload.put("response", defaultIfBlank(response, ""));
        emitEvent(emitter, "completed", completedPayload);
        emitter.complete();
      } catch (Exception e) {
        log.error("Streaming chat failed", e);
        try {
          Map<String, Object> errorPayload = new HashMap<>();
          errorPayload.put("message", defaultIfBlank(e.getMessage(), "Streaming failed"));
          emitEvent(emitter, "error", errorPayload);
        } catch (Exception emitException) {
          log.debug("Unable to emit SSE error event: {}", emitException.getMessage());
        } finally {
          // For SSE, complete the stream directly to avoid redispatching /error
          // with Content-Type text/event-stream.
          emitter.complete();
        }
      }
    });

    return emitter;
  }

  @Transactional(readOnly = true)
  public List<ChatMessage> getSessionHistory(Long sessionId, String username) {
    if (isGuestUser(username)) {
      return List.of();
    }

    ChatSession session = chatSessionRepository.findOwnedSessionWithUser(sessionId, username)
        .orElseThrow(() -> new SecurityException("You are not allowed to access this chat session"));

    return chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);
  }

  @Transactional(readOnly = true)
  public List<ChatSession> getUserSessions(String username, int limit) {
    if (isGuestUser(username)) {
      return List.of();
    }

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    int safeLimit = limit <= 0 ? 10 : limit;
    List<ChatSession> sessions = chatSessionRepository.findByUserOrderByCreatedAtDesc(user);
    return sessions.size() > safeLimit ? sessions.subList(0, safeLimit) : sessions;
  }

  @Transactional
  public ChatSession createChatSession(String username, String title) {
    if (isGuestUser(username)) {
      return createGuestSession(title);
    }

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    ChatSession session = new ChatSession();
    session.setUser(user);
    session.setTitle(title);
    session.setCreatedAt(LocalDateTime.now());
    session.setUpdatedAt(LocalDateTime.now());
    session.setActive(true);
    return chatSessionRepository.save(session);
  }

  private ChatSession getOrCreateSession(Long sessionId, String username, String seedText) {
    String title = truncate(seedText == null ? "New chat" : seedText, 80);

    if (isGuestUser(username)) {
      return createGuestSession(title);
    }

    if (sessionId == null) {
      return createChatSession(username, title);
    }

    return chatSessionRepository.findOwnedSessionWithUser(sessionId, username)
        .orElseThrow(() -> new SecurityException("You are not allowed to write to this chat session"));
  }

  private ChatMessage saveMessage(ChatSession session, String role, String content, String intent) {
    ChatMessage msg = new ChatMessage();
    msg.setSession(session);
    msg.setRole(role);
    msg.setContent(content);
    msg.setIntent(intent);
    msg.setCreatedAt(LocalDateTime.now());

    if (session == null || session.getId() == null) {
      return msg;
    }

    return chatMessageRepository.save(msg);
  }

  private String generateAssistantResponse(
      ChatSession session,
      String userMessage,
      String intent,
      ProgressSink progressSink) {

    if (progressSink != null) {
      progressSink.emit("retrieving_context", "Loading chat history and relevant context", 12);
    }

    return switch (intent) {
      case "LINK_VERIFICATION" -> handleLinkVerification(session, userMessage, progressSink);
      case "CURRENT_NEWS" -> handleCurrentNewsRequest(session, userMessage, progressSink);
      case "HOW_IT_WORKS" -> handleHowItWorks();
      case "HELP" -> handleHelp();
      default -> handleGeneralChat(session, userMessage, progressSink);
    };
  }

  private String handleLinkVerification(ChatSession session, String message, ProgressSink progressSink) {
    String url = extractFirstUrl(message);
    if (url == null) {
      return "I couldn't find a valid URL in your message. Please share a full article link starting with http:// or https://.";
    }

    if (progressSink != null) {
      progressSink.emit("checking_existing_verification", "Checking if this link was already verified", 20);
    }

    SynthesizedArticle existing = synthesizedArticleRepository.findBySourceUrl(url);
    if (existing != null) {
      return formatExistingVerification(existing);
    }

    if (progressSink != null) {
      progressSink.emit("scraping", "Scraping article content", 35);
    }

    ArticleScrapingService.ScrapedArticle scraped = articleScrapingService.scrapeArticle(url);
    if (scraped == null || isBlank(scraped.getContent())) {
      return "I couldn't extract content from that link. Please try another source URL.";
    }

    if (session.getUser() == null) {
      return "Link verification requires login because results are saved to your account.";
    }

    Article article = saveIncomingArticle(session.getUser(), scraped, url);

    if (progressSink != null) {
      progressSink.emit("extracting_claims", "Extracting claims from the article", 50);
    }

    List<Claim> claims = extractImportantClaims(scraped.getContent());

    if (claims.isEmpty()) {
      return "I could not extract verifiable claims from that link. Please try another source URL.";
    }

    if (progressSink != null) {
      progressSink.emit("extracting_claims", "Selected " + claims.size() + " high-priority claim(s) for verification", 56);
    }

    for (Claim claim : claims) {
      claim.setArticle(article);
    }
    claimRepository.saveAll(claims);

    if (progressSink != null) {
      progressSink.emit("verifying_claims", "Verifying top " + claims.size() + " claims with evidence", 64);
    }

    List<Verification> verifications = new ArrayList<>();
    for (int i = 0; i < claims.size(); i++) {
      Claim claim = claims.get(i);
      if (progressSink != null) {
        int percent = 64 + (int) Math.round(((i + 1) * 16.0) / Math.max(1, claims.size()));
        progressSink.emit(
            "verifying_claims",
            "Claim " + (i + 1) + "/" + claims.size() + ": " + conciseClaim(claim.getText()),
            percent);
      }

      Verification verification = verificationService.verifyClaim(claim.getText());
      verification.setClaim(claim);
      if (verification.getEvidence() != null) {
        for (Evidence ev : verification.getEvidence()) {
          ev.setVerification(verification);
        }
      }
      claim.setVerification(verification);
      verifications.add(verification);
    }
    verificationRepository.saveAll(verifications);

    if (progressSink != null) {
      progressSink.emit("synthesizing", "Generating verified summary", 86);
    }

    Synthesis synthesis = synthesisService.synthesizeVerifications(article, verifications);
    synthesis.setArticle(article);
    synthesisRepository.save(synthesis);

    Category category = resolveCategory(scraped.getTitle(), scraped.getContent());

    article.setCategory(category);
    article.setStatus("VERIFIED");
    article.setCredibilityScore(synthesis.getCredibilityScore());
    article.setVerifiedAt(LocalDateTime.now());
    article.setUpdatedAt(LocalDateTime.now());
    articleRepository.save(article);

    SynthesizedArticle synthesized = buildSynthesizedArticle(url, scraped, synthesis, claims, verifications, category);
    SynthesizedArticle savedSynth = synthesizedArticleRepository.save(synthesized);
    ragContextService.indexSynthesizedArticle(savedSynth);

    return formatFreshVerification(savedSynth);
  }

  private String handleCurrentNewsRequest(ChatSession session, String message, ProgressSink progressSink) {
    if (progressSink != null) {
      progressSink.emit("retrieving_context", "Planning web search strategy", 30);
    }

    String tavilyContext;
    try {
      NewsSearchPlan searchPlan = createNewsSearchPlan(session, message);

      if (progressSink != null) {
        progressSink.emit("retrieving_context", "Searching Tavily: " + truncate(searchPlan.query(), 70), 36);
      }

      if (searchPlan.shouldSearch()) {
        TavilySearchService.SearchResults searchResults = tavilySearchService.advancedSearch(
            searchPlan.query(),
            NEWS_SEARCH_RESULTS,
            normalizeSearchTopic(searchPlan.topic()));
        tavilyContext = formatTavilyResults(searchResults);
      } else {
        tavilyContext = "Web search intentionally skipped for this turn by planner. " +
            "Relying on internal context and recent conversation.";
      }
    } catch (Exception e) {
      log.warn("Tavily unavailable, falling back to internal context only: {}", e.getMessage());
      tavilyContext = "Live web search unavailable right now (Tavily key missing/invalid). " +
          "Using internal verified context and recent chat history instead.";
    }

    String ragContext = ragContextService.buildContext(message, 4);
    String historyContext = buildHistoryContext(session);

    String prompt = "User asked about current news as of " + LocalDateTime.now().toLocalDate() + ": " + message + "\n\n" +
        "Recent Web Findings:\n" + tavilyContext + "\n\n" +
        "RAG Context:\n" + ragContext + "\n\n" +
        "Recent Chat History:\n" + historyContext + "\n\n" +
        "Instructions:\n" +
        "- Respond naturally to the user's request, not with conversation analysis.\n" +
        "- Do not say things like 'you seem', 'it seems', or 'you said before'.\n" +
        "- Do not mention intent classification, internal reasoning, or training cutoff.\n" +
        "- Do not claim a fixed training cutoff date.\n" +
        "- For follow-up questions, keep the same subject unless the user explicitly changes topic.\n" +
        "- Use the provided Web Findings and RAG Context first.\n" +
        "- If evidence is limited, explicitly state uncertainty.\n" +
        "- Include top sources when available.";

    return getChatClient().prompt().user(prompt).call().content();
  }

  private String handleGeneralChat(ChatSession session, String message, ProgressSink progressSink) {
    if (progressSink != null) {
      progressSink.emit("retrieving_context", "Collecting related context", 26);
    }

    String historyContext = buildHistoryContext(session);
    String ragContext = ragContextService.buildContext(message, 4);

    String prompt = "User Message: " + message + "\n\n" +
        "Recent Chat History:\n" + historyContext + "\n\n" +
        "Relevant Verification Context:\n" + ragContext + "\n\n" +
        "Instructions:\n" +
        "- Respond directly to the user's latest message in a natural conversational tone.\n" +
        "- Do not analyze the user's intent or emotional state.\n" +
        "- Do not say things like 'you seem', 'it seems', or 'you said before'.\n" +
        "- Do not mention intent classification, internal reasoning, or training cutoff.\n" +
        "- Keep short acknowledgements (e.g., 'ok', 'great', 'thanks') brief and friendly.\n" +
        "- Respond as a news verification assistant.\n" +
        "- Do not claim a fixed training cutoff date.\n" +
        "- Prefer the provided context; if not sure, state uncertainty explicitly.";

    return getChatClient().prompt().user(prompt).call().content();
  }

  private String handleHowItWorks() {
    return "SATORN verifies content by scraping the source, extracting factual claims, checking evidence from trusted sources, and synthesizing a clear verdict with confidence and key findings.";
  }

  private String handleHelp() {
    return "You can ask for latest news context, share an article URL for verification, or ask follow-up questions about previously verified stories.";
  }

  private ChatClient getChatClient() {
    return llmFactory.getClientForTask(LLMFactory.TaskType.CHAT);
  }

  private String buildHistoryContext(ChatSession session) {
    if (session == null || session.getId() == null) {
      return "";
    }

    List<ChatMessage> recent = chatMessageRepository.findTop20BySessionOrderByCreatedAtDesc(session);
    Collections.reverse(recent);

    StringBuilder history = new StringBuilder();
    for (ChatMessage msg : recent) {
      history.append(msg.getRole()).append(": ")
          .append(truncate(msg.getContent(), 220)).append("\n");
    }
    return history.toString();
  }

  private Category resolveCategory(String title, String content) {
    CategoryTaggingService.CategoryTaggingResult categoryResult = categoryTaggingService
        .categorizeArticle(title, content);

    String categoryName = categoryResult.getPrimaryCategory().getDisplayName();
    return categoryRepository.findByName(categoryName)
        .orElseGet(() -> {
          Category category = new Category();
          category.setName(categoryName);
          category.setColor(categoryResult.getPrimaryCategory().getColor());
          return categoryRepository.save(category);
        });
  }

  private Article saveIncomingArticle(User user, ArticleScrapingService.ScrapedArticle scraped, String url) {
    Article article = Article.builder()
        .url(url)
        .title(defaultIfBlank(scraped.getTitle(), "Untitled Article"))
        .content(defaultIfBlank(scraped.getContent(), ""))
        .author(scraped.getAuthor())
        .status("PROCESSING")
        .submittedBy(user)
        .submittedAt(LocalDateTime.now())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    return articleRepository.save(article);
  }

  private SynthesizedArticle buildSynthesizedArticle(
      String sourceUrl,
      ArticleScrapingService.ScrapedArticle scraped,
      Synthesis synthesis,
      List<Claim> claims,
      List<Verification> verifications,
      Category category) {

    long trueClaims = verifications.stream().filter(this::isTrueLike).count();
    long falseClaims = verifications.stream().filter(this::isFalseLike).count();
    long unverifiable = verifications.stream().filter(v -> "UNVERIFIABLE".equalsIgnoreCase(v.getVerdict())).count();

    return SynthesizedArticle.builder()
        .title(defaultIfBlank(scraped.getTitle(), "Untitled Article"))
        .originalContent(scraped.getContent())
        .synthesizedNarrative(synthesis.getSummary())
        .sourceUrl(sourceUrl)
        .originalSource(sourceUrl)
        .rssFeedSource("CHAT_VERIFICATION")
        .author(defaultIfBlank(scraped.getAuthor(), "Unknown"))
        .imageUrl(scraped.getImageUrl())
        .publishDate(LocalDateTime.now())
        .credibilityScore(synthesis.getCredibilityScore())
        .status("VERIFIED")
        .verdict(toVerdictLabel(synthesis.getOverallVerdict()))
        .category(category)
        .keyFindings(synthesis.getKeyFindings())
        .timeline("Timeline generated during verification")
        .claimsCount(claims.size())
        .verifiedClaimsCount(verifications.size())
        .trueClaims((int) trueClaims)
        .falseClaims((int) falseClaims)
        .unverifiableClaims((int) unverifiable)
        .viewCount(0L)
        .isTrending(false)
        .build();
  }

  private boolean isTrueLike(Verification verification) {
    String verdict = safeUpper(verification.getVerdict());
    return verdict.equals("VERIFIED") || verdict.equals("PARTIALLY_VERIFIED") || verdict.equals("MOSTLY_TRUE");
  }

  private boolean isFalseLike(Verification verification) {
    String verdict = safeUpper(verification.getVerdict());
    return verdict.equals("CONTRADICTED") || verdict.equals("MOSTLY_FALSE") || verdict.equals("FALSE");
  }

  private String toVerdictLabel(String overallVerdict) {
    String verdict = safeUpper(overallVerdict);
    return switch (verdict) {
      case "CREDIBLE", "MOSTLY_CREDIBLE" -> "MOSTLY_TRUE";
      case "MOSTLY_UNRELIABLE", "NOT_CREDIBLE" -> "MOSTLY_FALSE";
      default -> "MIXED";
    };
  }

  private String formatExistingVerification(SynthesizedArticle article) {
    return "This link was already verified.\n\n" +
        "Title: " + defaultIfBlank(article.getTitle(), "N/A") + "\n" +
        "Verdict: " + defaultIfBlank(article.getVerdict(), "N/A") + "\n" +
        "Credibility Score: " + defaultIfBlank(article.getCredibilityScore(), "N/A") + "\n\n" +
        defaultIfBlank(article.getSynthesizedNarrative(), "Summary unavailable.");
  }

  private String formatFreshVerification(SynthesizedArticle article) {
    return "Verification completed.\n\n" +
        "Title: " + defaultIfBlank(article.getTitle(), "N/A") + "\n" +
        "Verdict: " + defaultIfBlank(article.getVerdict(), "N/A") + "\n" +
        "Credibility Score: " + defaultIfBlank(article.getCredibilityScore(), "N/A") + "\n" +
        "Claims Checked: " + defaultIfBlank(article.getClaimsCount(), 0) + "\n\n" +
        defaultIfBlank(article.getSynthesizedNarrative(), "Summary unavailable.");
  }

  private String formatTavilyResults(TavilySearchService.SearchResults results) {
    if (results == null || results.getResults() == null || results.getResults().isEmpty()) {
      return "No fresh sources found.";
    }

    StringBuilder sb = new StringBuilder();
    int idx = 1;
    for (TavilySearchService.SearchResult result : results.getResults()) {
      sb.append(idx++)
          .append(". ")
          .append(defaultIfBlank(result.getTitle(), "Untitled"))
          .append("\n")
          .append("   URL: ").append(defaultIfBlank(result.getUrl(), "N/A")).append("\n")
          .append("   Snippet: ").append(truncate(defaultIfBlank(result.getContent(), ""), 250)).append("\n");
    }

    return sb.toString();
  }

  private String detectIntent(String message) {
    String safeMessage = defaultIfBlank(message, "");

    try {
      String prompt = """
          Classify the user's intent into exactly one label from this closed set:
          LINK_VERIFICATION, CURRENT_NEWS, HOW_IT_WORKS, HELP, GENERAL_CHAT.

          Rules:
          - LINK_VERIFICATION: user provides or asks to verify a URL/article link.
          - CURRENT_NEWS: user asks for latest/current/recent updates, trends, or time-bounded updates (for example "as of Feb 2026").
          - HOW_IT_WORKS: user asks how the system/process works.
          - HELP: user asks for usage guidance/help commands.
          - GENERAL_CHAT: everything else.

          Output strictly a single label only. No extra text.
          User message: %s
          """.formatted(safeMessage);

      String raw = defaultIfBlank(getChatClient().prompt().user(prompt).call().content(), "").trim();
      String normalized = raw.replace("\"", "")
          .replace("`", "")
          .replace("-", "_")
          .trim()
          .toUpperCase(Locale.ROOT);

      if (ALLOWED_INTENTS.contains(normalized)) {
        return normalized;
      }

      log.warn("LLM intent classifier returned unexpected label '{}', defaulting to GENERAL_CHAT", raw);
    } catch (Exception e) {
      log.warn("LLM intent classifier failed, defaulting to GENERAL_CHAT: {}", e.getMessage());
    }

    return "GENERAL_CHAT";
  }

  private String extractFirstUrl(String message) {
    if (message == null) {
      return null;
    }

    Matcher matcher = URL_PATTERN.matcher(message);
    if (!matcher.find()) {
      return null;
    }

    String url = matcher.group(1);
    while (url.endsWith(",") || url.endsWith(".") || url.endsWith(")")) {
      url = url.substring(0, url.length() - 1);
    }
    return url;
  }

  private NewsSearchPlan createNewsSearchPlan(ChatSession session, String userMessage) {
    String historyContext = buildHistoryContext(session);

    try {
      String plannerPrompt = """
          You are a web-search planner for a news verification assistant.

          Task:
          - Decide whether Tavily web search should be called for this turn.
          - Generate ONE high-quality query grounded in conversation context.
          - Resolve follow-ups (pronouns like "these", "those", "new additions", "what date") using chat history topic.

          Constraints:
          - Keep topic continuity unless user explicitly switches subject.
          - If user asks for dates/timeline updates, include the same entity/topic from history in the query.
          - Prefer precision over broad queries.
          - topic must be either "news" or "general".

          Return STRICT JSON only:
          {
            "shouldSearch": true,
            "query": "string",
            "topic": "news",
            "reason": "short string"
          }

          Conversation history:
          %s

          Latest user message:
          %s
          """.formatted(historyContext, defaultIfBlank(userMessage, ""));

      String plannerRaw = defaultIfBlank(getChatClient().prompt().user(plannerPrompt).call().content(), "");
      JsonNode json = ResponseParser.parseJsonNode(plannerRaw);

      boolean shouldSearch = json.path("shouldSearch").asBoolean(true);
      String query = json.path("query").asText("").trim();
      String topic = json.path("topic").asText("news").trim().toLowerCase(Locale.ROOT);

      if (query.isBlank()) {
        query = defaultIfBlank(userMessage, "");
      }

      if (query.isBlank()) {
        query = "latest verified news updates";
      }

      return new NewsSearchPlan(shouldSearch, query, topic);
    } catch (Exception e) {
      log.warn("News search planner failed, using direct fallback query: {}", e.getMessage());
      return new NewsSearchPlan(true, defaultIfBlank(userMessage, "latest news updates"), "news");
    }
  }

  private String normalizeSearchTopic(String topic) {
    if ("general".equalsIgnoreCase(topic)) {
      return "general";
    }
    return "news";
  }

  private List<Claim> selectTopClaims(List<Claim> claims, int maxClaims) {
    if (claims == null || claims.isEmpty()) {
      return List.of();
    }

    return claims.stream()
        .sorted((a, b) -> Integer.compare(parseImportance(b), parseImportance(a)))
        .limit(Math.max(1, maxClaims))
        .toList();
  }

  private List<Claim> extractImportantClaims(String articleContent) {
    List<Claim> allClaims = claimExtractionService.extractClaims(articleContent);
    if (allClaims == null || allClaims.isEmpty()) {
      return List.of();
    }

    List<Claim> ranked = selectTopClaims(allClaims, allClaims.size());
    int[] thresholds = { 8, 7, 6, 5 };

    for (int threshold : thresholds) {
      List<Claim> selected = ranked.stream()
          .filter(c -> parseImportance(c) >= threshold)
          .limit(MAX_LINK_VERIFICATION_CLAIMS)
          .toList();
      if (!selected.isEmpty()) {
        return selected;
      }
    }

    // Fallback: if all claims are low-confidence/low-importance, verify only the single strongest one.
    return ranked.stream().limit(1).toList();
  }

  private int parseImportance(Claim claim) {
    if (claim == null || claim.getImportance() == null || claim.getImportance().isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(claim.getImportance().trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private String conciseClaim(String claimText) {
    if (claimText == null || claimText.isBlank()) {
      return "Analyzing claim";
    }
    String normalized = claimText.replaceAll("\\s+", " ").trim();
    return truncate(normalized, 80);
  }

  private boolean isGuestUser(String username) {
    return username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username);
  }

  private ChatSession createGuestSession(String title) {
    ChatSession session = new ChatSession();
    session.setTitle(title == null || title.isBlank() ? "Guest Chat" : title);
    session.setCreatedAt(LocalDateTime.now());
    session.setUpdatedAt(LocalDateTime.now());
    session.setActive(true);
    session.setUser(null);
    return session;
  }

  private void streamTextResponse(SseEmitter emitter, String response) {
    if (response == null || response.isBlank()) {
      return;
    }

    String[] chunks = response.split("\\s+");
    for (String chunk : chunks) {
      emitEvent(emitter, "token", chunk + " ");
    }
  }

  private void emitProgress(SseEmitter emitter, String stage, String message, int percent) {
    emitEvent(emitter, "progress", Map.of(
        "stage", stage,
        "message", message,
        "percent", percent));
  }

  private void emitEvent(SseEmitter emitter, String eventName, Object data) {
    try {
      if ("token".equals(eventName)) {
        // Use default SSE message event for maximum frontend compatibility
        emitter.send(data);
        return;
      }
      emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
    } catch (IOException e) {
      throw new RuntimeException(e);
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

  private String safeUpper(String value) {
    return value == null ? "" : value.toUpperCase(Locale.ROOT);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String defaultIfBlank(String value, String fallback) {
    return isBlank(value) ? fallback : value;
  }

  private Object defaultIfBlank(Object value, Object fallback) {
    if (value == null) {
      return fallback;
    }
    if (value instanceof String s) {
      return s.isBlank() ? fallback : s;
    }
    return value;
  }

  @FunctionalInterface
  private interface ProgressSink {
    void emit(String stage, String message, int percent);
  }

  private record NewsSearchPlan(boolean shouldSearch, String query, String topic) {
  }

  @lombok.Data
  @lombok.AllArgsConstructor
  public static class ChatResponse {
    private Long sessionId;
    private String userMessage;
    private String aiResponse;
    private String intent;
  }
}
