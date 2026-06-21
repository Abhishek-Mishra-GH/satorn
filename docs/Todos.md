# Todos

## How to Use
- Mark `[x]` when done.
- Keep PR/commit id next to completed items.
- Do not start P1/P2 before P0 stability tasks are merged.

## P0 - Stability and Correctness
- [ ] Turn on Flyway and disable `ddl-auto` for non-local envs.
- [ ] Add migration for `saved_articles` table + indexes.
- [ ] Resolve `syntheses` vs `synthesis` naming mismatch with migration and entity alignment.
- [ ] Remove/replace `RssSchemaInitializer` runtime schema bootstrap.
- [ ] Fix claim-verification relation persistence (`claim_id` binding) and add regression tests.
- [ ] Add global exception handling (`@RestControllerAdvice`) and consistent error payload.
- [ ] Add integration test for article verification happy path.
- [ ] Add integration test for verification failure path.

## P1 - Pivot Features (Curated Feed + On-Demand Verification)
- [ ] Create `user_news_preferences` schema + entity + repository.
- [ ] Create `user_news_signals` schema + entity + repository.
- [ ] Create `verification_requests` schema + entity + repository.
- [ ] Build `FeedRankingService` (separate from `LearningService`).
- [ ] Add `GET /api/feed/curated` endpoint.
- [ ] Add `PUT /api/feed/preferences` endpoint.
- [ ] Add `POST /api/feed/signals` endpoint.
- [ ] Add `POST /api/verification/requests` endpoint.
- [ ] Add `GET /api/verification/requests/{id}` endpoint.
- [ ] Add background worker for verification job execution with retry/backoff.
- [ ] Add idempotency key using normalized URL hash.
- [ ] Add authorization checks so users can only see their own verification requests.

## P1 - Refactor for Maintainability
- [ ] Split `LearningService` into profile/recommendation/quiz/tutor services.
- [ ] Split `RssPreVerificationQueueService` into intake, prioritization, processing components.
- [ ] Split `ChatService` into intent, orchestration, verification, and news handlers.
- [ ] Replace controller `Map<String,Object>` responses with DTOs.
- [ ] Remove per-controller `@CrossOrigin("*")` and rely on centralized CORS config.

## P2 - Performance and Observability
- [ ] Make view count increment atomic in repository query.
- [ ] Add `@Cacheable` to read-heavy feed/article endpoints.
- [ ] Add queue and verification latency metrics (Micrometer).
- [ ] Add provider error-rate metrics (Groq/Tavily/OpenAI/Ollama).
- [ ] Add structured request correlation id logging.
- [ ] Add load test scenario for curated feed and verification request endpoints.

## P2 - Interview Presentation Assets
- [ ] Update architecture diagram for pivoted flow.
- [ ] Add sequence diagram: user request -> verification request -> async worker -> result.
- [ ] Add performance before/after numbers (latency, throughput).
- [ ] Add test coverage summary in README.
- [ ] Prepare 3 incident/debug stories from real implementation challenges.

## Done Log
- [ ] (date) initialized docs and pivot backlog
