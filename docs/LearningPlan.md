# LearningPlan

## Objective
Use this project to become interview-ready for backend roles focused on scalable systems, API design, async processing, and AI-assisted workflows.

## Key Technologies Already in This Project
- Java 21, Spring Boot 3
- Spring Security + JWT auth
- Spring Data JPA + PostgreSQL
- Redis + Spring Cache
- Flyway migration artifacts
- AI orchestration (Spring AI, Groq/OpenAI/Ollama via `LLMFactory`)
- External integrations (Tavily, Jsoup scraping)
- Async/background processing (`@Scheduled`, queue workers, `CompletableFuture`)
- Docker + docker-compose

## 6-Week Interview Prep Plan

### Week 1: Data Modeling and Migration Discipline
- Learn:
  - transactional boundaries, schema versioning, migration rollback strategy
  - entity vs migration drift detection
- Do in this project:
  - enable Flyway-first flow and remove `ddl-auto` dependence
  - add missing migrations (`saved_articles`, `syntheses/synthesis` alignment)
- Interview output:
  - explain how you prevented environment-specific schema bugs

### Week 2: API Contracts and Error Handling
- Learn:
  - DTO boundaries, validation groups, global exception handling, problem details
- Do in this project:
  - replace map-based controller responses with typed DTOs
  - add `@RestControllerAdvice` with stable error codes
- Interview output:
  - explain how API contract stability improved client reliability

### Week 3: Scalable Async Verification Pipeline
- Learn:
  - queue patterns, idempotency keys, retry/backoff, dead-letter strategy
- Do in this project:
  - implement `verification_requests` + async worker pipeline
  - process verification only on explicit user request
- Interview output:
  - explain why on-demand verification is cheaper and more scalable than blanket pre-verification

### Week 4: Personalization and Ranking
- Learn:
  - recommendation ranking basics, user-signal weighting, exploration vs exploitation
- Do in this project:
  - add `user_news_preferences` and `user_news_signals`
  - implement `GET /api/feed/curated` with ranking explanation fields
- Interview output:
  - explain ranking features and trade-offs (freshness vs credibility vs user interest)

### Week 5: Testing and Reliability
- Learn:
  - unit vs integration test strategy, Testcontainers, contract tests
- Do in this project:
  - add tests for verification workflow, ranking service, and auth boundaries
  - target meaningful coverage on critical flows (not vanity %)
- Interview output:
  - explain how tests caught regressions in async and persistence workflows

### Week 6: Observability and Performance Story
- Learn:
  - latency budgets, queue depth metrics, p95/p99, cache hit ratio, structured logs
- Do in this project:
  - add Micrometer metrics and dashboards for queue + verification latency
  - measure before/after latency for curated feed and verification APIs
- Interview output:
  - present concrete performance numbers and bottleneck fixes

## Daily Routine (60-90 Minutes)
- 20 min: read one core backend concept (transactions, queues, indexing, caching).
- 30-45 min: implement one small project improvement.
- 10-15 min: write interview notes (problem, design, trade-off, metric).
- 10 min: practice one verbal explanation aloud.

## Interview Story Bank (Prepare These)
- Story 1: Migrated from schema drift to migration-first reliability.
- Story 2: Pivoted architecture from global verification to user-triggered verification pipeline.
- Story 3: Added personalization ranking using user preferences and behavioral signals.
- Story 4: Broke down monolithic service class into testable components.
- Story 5: Improved latency/cost with async jobs, caching, and provider fallback strategy.

## Must-Know Questions From This Project
- Why choose async verification over synchronous in request path?
- How do you guarantee idempotency for repeated URL verification requests?
- How do you handle eventual consistency between request acceptance and result availability?
- How do you rank curated feed items with sparse user history (cold start)?
- How do you monitor and tune provider failures/latency (Groq, Tavily, OpenAI)?
- How do you secure mixed public/private endpoints without overexposing data?

## Exit Criteria (Interview Ready)
- You can whiteboard your system in 5-7 minutes.
- You can justify 3 major trade-offs with metrics.
- You can show tests for critical workflows.
- You can explain one production-like incident and how you fixed it.
- You can defend your pivot architecture as scalable and cost-aware.
