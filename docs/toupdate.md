# Server Refactor and Pivot Update Plan

## Goal
Shift from platform-wide pre-verification to:
1. personalized, user-preference curated feed
2. verification only when user asks for an article/link

## High-Impact Updates (P0)

- [ ] **Unify database strategy (must fix first)**
  - Current issue: `spring.jpa.hibernate.ddl-auto=update` and `spring.flyway.enabled=false` are active, so schema is drifting by environment.
  - Evidence: `server/src/main/resources/application.yaml`.
  - Action: enable Flyway, disable `ddl-auto` in non-local environments, and move every schema change to migration scripts.

- [ ] **Fix schema drift and table-name mismatches**
  - Current issue: entity table `syntheses` vs migration table `synthesis`; `saved_articles` entity exists but no migration table.
  - Evidence: `server/src/main/java/com/_xdev/satorn/domain/entity/Synthesis.java`, `server/src/main/resources/db/migration/V1__initial_schema.sql`, `server/src/main/java/com/_xdev/satorn/domain/entity/SavedArticle.java`.
  - Action: add migration(s) for canonical tables and constraints; stop relying on auto DDL.

- [ ] **Fix verification persistence integrity**
  - Current issue: `Verification` requires `claim_id` (`nullable = false`), but `ArticleVerificationService.verifyArticle` saves verifications without explicitly binding claim/evidence relations.
  - Evidence: `server/src/main/java/com/_xdev/satorn/service/ai/ArticleVerificationService.java`, `server/src/main/java/com/_xdev/satorn/domain/entity/Verification.java`.
  - Action: set `verification.setClaim(claim)`, set reverse relations, persist through aggregate root with tests.

- [ ] **Remove runtime schema bootstrapping logic for core tables**
  - Current issue: ad-hoc schema bootstrap via code makes infra non-deterministic.
  - Evidence: `server/src/main/java/com/_xdev/satorn/config/RssSchemaInitializer.java`.
  - Action: replace with Flyway-managed migrations + idempotent seed strategy.

- [ ] **Replace giant service/controller classes with bounded modules**
  - Current issue: very large files hurt testability and interview-level maintainability story.
  - Evidence:
    - `LearningService.java` (~1489 lines)
    - `RssPreVerificationQueueService.java` (~1227 lines)
    - `ChatService.java` (~846 lines)
    - `SynthesizedArticleController.java` (~660 lines)
  - Action: split by capability (ranking, quiz generation, tutor, queue intake, queue worker, verification orchestration).

## Pivot-Specific Product + Architecture Updates (P1)

- [ ] **Introduce explicit user preference model for curated feed**
  - Current gap: no dedicated news preference schema; only learning profile and saved articles.
  - Action: add tables such as:
    - `user_news_preferences` (topics, source trust list, language, region, recency window)
    - `user_news_signals` (impression, click, save, hide, open_duration)
    - `user_feed_state` (last served cursor/version)

- [ ] **Change ingestion to candidate-first, verify-on-demand**
  - Current gap: system still performs broad RSS pre-verification.
  - Evidence: `RssFeedMonitoringService`, `RssPreVerificationQueueService`.
  - Action: ingest metadata/snippets cheaply, rank for each user, and verify only on explicit user request or high-priority trigger.

- [ ] **Add verification request workflow**
  - Action: add `verification_requests` + `verification_jobs` for async processing with idempotency (`normalized_url_hash`) and status lifecycle.
  - New API shape:
    - `POST /api/verification/requests`
    - `GET /api/verification/requests/{id}`
    - `GET /api/feed/curated`
    - `PUT /api/feed/preferences`

- [ ] **Decouple recommendation logic from learning module**
  - Current gap: recommendation lives in `LearningService`; product pivot needs a dedicated feed-ranking pipeline.
  - Action: create `FeedRankingService` and keep learning-focused ranking separate.

## Security, API Quality, and Reliability (P1)

- [ ] **Harden API contracts**
  - Current gap: many endpoints return ad-hoc `Map<String,Object>` responses and catch broad `Exception`.
  - Action: introduce typed response DTOs + global exception handler (`@RestControllerAdvice`) + error codes.

- [ ] **Revisit public access boundaries and CORS**
  - Current gap: broad `permitAll` for `/api/chat/**` and wildcard `@CrossOrigin("*")` at controller level.
  - Action: centralize CORS in one place, remove per-controller wildcard, define explicit auth rules per endpoint.

- [ ] **Use bounded executors for async work**
  - Current gap: multiple `CompletableFuture.runAsync/supplyAsync` calls use common pool.
  - Action: define dedicated task executors for queue processing and SSE tasks; set concurrency and backpressure limits.

## Performance + Scale (P2)

- [ ] **Make view count updates atomic**
  - Current gap: read-modify-write in `getSynthesizedArticle` can lose increments under concurrency.
  - Action: add atomic DB update query (`increment view_count where id=?`).

- [ ] **Use caching where configured**
  - Current gap: cache types are configured, but `@Cacheable` usage is minimal.
  - Action: add read-path caching for curated feed slices, article detail, and preference snapshots with targeted evictions.

- [ ] **Add observability for interview-grade operations**
  - Action: add metrics for queue depth, verification latency, provider failure rate, ranking latency, and feed response time (Micrometer + dashboards).

## Test and Delivery Gaps (P0/P1)

- [ ] **Expand test suite beyond context load**
  - Current gap: only `SatornApplicationTests` exists.
  - Action:
    - service tests for ranking/verification
    - repository tests for feed queries
    - controller slice tests for auth and validation
    - integration tests using Testcontainers (Postgres + Redis)

- [ ] **Add CI quality gates**
  - Action: pipeline with `mvn test`, static analysis, and migration validation before merge.

## Quick Interview Wins to Implement First

- [ ] Flyway-first schema cleanup (drift + migrations)
- [ ] verification request async pipeline with idempotency
- [ ] curated feed endpoint using user preferences + signals
- [ ] 10-20 meaningful automated tests (service + integration)
- [ ] metrics dashboard screenshot + load test numbers for README
