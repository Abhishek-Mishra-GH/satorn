# SATORN: AI-Powered Current Affairs Learning Platform

**Hackathon Theme:** AI-Powered Online Education

SATORN is a hackathon project focused on online education for competitive exam aspirants.
It converts live current affairs into structured learning through verification, tutoring, and adaptive quizzes.

**[Live Link](https://satorn-psi.vercel.app)** | **[Demo Video](https://youtu.be/t_4WkHUcttk?si=O59apPMBnpczeQKm)**

## Introduction

Most aspirants lose time in three places:
- figuring out what news is actually reliable
- turning raw headlines into exam-ready understanding
- measuring what they know vs what they only recognize

SATORN solves this by combining:
- live retrieval (Tavily)
- verification and synthesis
- RAG-backed contextual learning
- tutor + adaptive quiz + skill tracking

In short: SATORN is a verified current affairs learning engine, not just a news app.

## API Documentation

Swagger is intentionally exposed so judges can inspect and test all routes directly.

- Swagger UI:
  - `http://localhost:8080/swagger-ui/index.html`
  - `https://satorn-server-production-bae1.up.railway.app/swagger-ui/index.html`
- OpenAPI JSON:
  - `http://localhost:8080/v3/api-docs`
  - `https://satorn-server-production-bae1.up.railway.app/v3/api-docs`

## Installation and Setup

This section is kept early for judges to run and evaluate quickly.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop
- API keys:
  - Groq
  - Tavily
  - OpenAI (optional fallback/provider path)

## Option A: Docker (Fastest Evaluation Path)

1. Configure backend environment:

```bash
cd server
cp .env.example .env
```

2. Start infrastructure:

```bash
docker compose up -d postgres redis
```

3. Run backend:

```bash
mvn spring-boot:run
```

4. Optional full stack in Docker:

```bash
docker compose --profile fullstack up --build
```

## Option B: Local Run (Backend + Frontend)

Backend:

```bash
cd server
cp .env.example .env
mvn spring-boot:run
```

Frontend:

```bash
cd client
npm install
npm run dev
```

## Verification URLs

- Backend health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Deployed Swagger UI: `https://satorn-server-production-bae1.up.railway.app/swagger-ui/index.html`
- Deployed OpenAPI: `https://satorn-server-production-bae1.up.railway.app/v3/api-docs`
- Frontend (Vite): typically `http://localhost:5173`


## What Makes SATORN Different

### Education-first, not feed-first

SATORN is built around exam preparation outcomes:
- conceptual clarity
- topic retention
- answer framing
- measurable skill progression

### Trust layer before learning layer

Instead of tutoring from raw web snippets, SATORN:
1. verifies claims
2. synthesizes context
3. then uses that in learning workflows

### Live-current quiz generation

Quizzes are generated from latest developments using:
- Tavily news retrieval
- LLM question generation
- fallback to verified internal corpus if live sources are sparse

This gives both freshness and resilience.

## High-Level Architecture

```text
                    +------------------------------+
                    |   Frontend (React + Vite)   |
                    +--------------+---------------+
                                   |
                                   v
                  +----------------+----------------+
                  | Spring Boot Backend (REST API) |
                  +----------------+----------------+
                                   |
          +------------------------+-------------------------+
          |                        |                         |
          v                        v                         v
 +------------------+   +----------------------+   +--------------------+
 | Verification     |   | Learning Engine      |   | Chat + Tutor       |
 | claim->evidence  |   | profile->quiz->skill |   | RAG + LLM context  |
 +--------+---------+   +----------+-----------+   +---------+----------+
          |                        |                         |
          +-------------------+----+-------------------------+
                              |
                              v
                +-------------------------------+
                | PostgreSQL + pgvector + Redis |
                +-------------------------------+
                              |
                              v
                 +-----------------------------+
                 | Tavily + RSS + LLM providers|
                 +-----------------------------+
```

## Technology Stack (Complete)

## Backend Core

- Java 21
- Spring Boot 3.2.1
- Spring Web (`spring-boot-starter-web`)
- Spring Data JPA (`spring-boot-starter-data-jpa`)
- Spring Validation (`spring-boot-starter-validation`)
- Lombok

## AI + Retrieval

- Spring AI 1.0.0-M4
- Spring AI OpenAI starter
- Spring AI pgvector store starter
- RAG with PostgreSQL pgvector
- LLM provider routing via `LLMFactory`:
  - Groq (primary high-throughput path)
  - OpenAI
  - Local OpenAI-compatible endpoint (Ollama mode)
- Tavily Search API for:
  - live evidence retrieval
  - latest current affairs for quiz generation

## Data + Infrastructure

- PostgreSQL
- pgvector Java driver (`com.pgvector:pgvector`)
- Redis (`spring-boot-starter-data-redis`)
- Spring Cache (`spring-boot-starter-cache`)
- Docker + Docker Compose
- Multi-stage Docker backend build (Maven -> JRE)

## Security + Auth

- Spring Security (`spring-boot-starter-security`)
- JWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- Role-based access control (RBAC)

## API and Observability

- OpenAPI/Swagger (`springdoc-openapi-starter-webmvc-ui`)
- Spring Boot Actuator (`spring-boot-starter-actuator`)
- Structured logging via Spring logging configuration

## Data Processing + Integrations

- Jsoup for article scraping
- Apache HttpClient 5
- Jackson Databind for JSON parsing/mapping
- Flyway (schema migration artifacts in project)

## Build + Testing

- Maven build tool
- Spring Boot Maven plugin
- Spring Boot Test (`spring-boot-starter-test`)
- Spring Security Test (`spring-security-test`)

## Frontend (supporting stack, minimal)

- React 19 + Vite + TypeScript
- React Router DOM
- TanStack Query
- Zustand
- Tailwind CSS v4
- Axios
- React Hook Form + Zod

## Core Backend Flows

## 1) RSS and Article Ingestion

Primary services:
- `server/src/main/java/com/_xdev/satorn/service/feed/RssFeedMonitoringService.java`
- `server/src/main/java/com/_xdev/satorn/service/feed/RssPreVerificationQueueService.java`
- `server/src/main/java/com/_xdev/satorn/service/external/ArticleScrapingService.java`

Purpose:
- collect fresh articles
- normalize content
- enqueue for verification

## 2) Verification Pipeline

Primary services:
- `server/src/main/java/com/_xdev/satorn/service/ai/ClaimExtractionService.java`
- `server/src/main/java/com/_xdev/satorn/service/ai/VerificationService.java`
- `server/src/main/java/com/_xdev/satorn/service/ai/SynthesisService.java`
- `server/src/main/java/com/_xdev/satorn/service/external/TavilySearchService.java`

Purpose:
- extract claims
- retrieve live evidence
- reason over support/contradiction
- produce verdict + credibility + narrative

## 3) RAG Context Layer

Primary service:
- `server/src/main/java/com/_xdev/satorn/service/ai/RagContextService.java`

Purpose:
- retrieve semantically relevant verified context
- power chat, tutor, and downstream learning responses

## 4) Learning Layer

Primary service:
- `server/src/main/java/com/_xdev/satorn/service/learning/LearningService.java`

Features:
- learner profile
- recommendation ranking
- adaptive quiz generation
- quiz scoring and feedback
- skill progress updates
- tutor responses with exam-angle framing

Quiz strategy transparency:
- `generationStrategy = TAVILY_NEWS_AI` (primary)
- `generationStrategy = INTERNAL_VERIFIED_FALLBACK` (fallback)

## 5) Tutor and Conversational Guidance

Primary services:
- `server/src/main/java/com/_xdev/satorn/service/learning/LearningService.java`
- `server/src/main/java/com/_xdev/satorn/service/ai/ChatService.java`

Purpose:
- explain current developments in exam-relevant format
- provide next actions and learning continuity

## Key API Groups

### Learning APIs

- `GET /api/learning/profile`
- `PUT /api/learning/profile`
- `GET /api/learning/recommendations`
- `POST /api/learning/quiz/generate`
- `POST /api/learning/quiz/submit`
- `GET /api/learning/skills`
- `POST /api/learning/tutor`

### Verification and Content APIs

- `GET /api/synthesized-articles`
- `GET /api/synthesized-articles/{id}`
- `GET /api/synthesized-articles/trending`
- `GET /api/synthesized-articles/top-credible`
- `GET /api/synthesized-articles/search`

### Chat APIs

- `POST /api/chat`
- `POST /api/chat/stream`
- `GET /api/chat/sessions`
- `GET /api/chat/sessions/{sessionId}`

### Admin RSS APIs

- `GET /api/admin/rss-feeds`
- `POST /api/admin/rss-feeds`
- `POST /api/admin/rss-feeds/process-all`
- `POST /api/admin/rss-feeds/process-queue`
- `GET /api/admin/rss-feeds/queue-status`

## Security and Roles

- JWT-based stateless auth
- role-aware endpoint protection
- method-level authorization in controllers/services

Typical role split:
- `ROLE_USER`: learning workflows
- `ROLE_ADMIN`: admin controls and management

### Endpoint Access Matrix (Judge View)

| Endpoint Group | Auth Required | Role Requirement |
|---|---|---|
| `/api/auth/**` | No | Public |
| `/v3/api-docs/**`, `/swagger-ui/**` | No | Public |
| `/actuator/health` | No | Public |
| `/api/articles/**` | No | Public |
| `/api/synthesized-articles/**` | No | Public |
| `/api/chat/**` | No | Public |
| `/api/users/**` | Yes | `ROLE_USER` |
| `/api/learning/**` | Yes | `ROLE_USER` or `ROLE_ADMIN` |
| `/api/admin/**` | Yes | `ROLE_ADMIN` or `ROLE_MODERATOR` |
| Other non-public routes | Yes | Authenticated user |

Notes:
- Access rules are enforced in `server/src/main/java/com/_xdev/satorn/config/SecurityConfig.java`.
- Learning endpoints additionally use method-level checks in `server/src/main/java/com/_xdev/satorn/controller/LearningController.java`.

## Backend File Structure (Detailed)

```text
server/
  src/main/java/com/_xdev/satorn/
    SatornApplication.java

    ai/
      config/
        LLMFactory.java
      prompt/
        PromptTemplates.java
      util/
        ResponseParser.java

    config/
      CacheConfiguration.java
      CorsConfig.java
      DataSeeder.java
      OpenApiConfig.java
      RestClientConfig.java
      RssSchemaInitializer.java
      SecurityConfig.java

    controller/
      AdminController.java
      ArticleController.java
      AuthController.java
      ChatController.java
      LearningController.java
      RssFeedConfigController.java
      SynthesizedArticleController.java
      UserController.java

    domain/
      entity/
        Article.java
        Category.java
        ChatMessage.java
        ChatSession.java
        Claim.java
        Evidence.java
        LearnerProfile.java
        LearningQuizQuestion.java
        LearningQuizSession.java
        LearningSkillProgress.java
        RefreshToken.java
        Role.java
        RssFeedConfig.java
        SavedArticle.java
        Synthesis.java
        SynthesizedArticle.java
        SynthesizedArticleClaim.java
        User.java
        Verification.java
      repository/
        ArticleRepository.java
        CategoryRepository.java
        ChatMessageRepository.java
        ChatSessionRepository.java
        ClaimRepository.java
        EvidenceRepository.java
        LearnerProfileRepository.java
        LearningQuizQuestionRepository.java
        LearningQuizSessionRepository.java
        LearningSkillProgressRepository.java
        RefreshTokenRepository.java
        RoleRepository.java
        RssFeedConfigRepository.java
        SavedArticleRepository.java
        SynthesisRepository.java
        SynthesizedArticleRepository.java
        UserRepository.java
        VerificationRepository.java

    dto/
      ArticleRequest.java
      ArticleResponse.java
      ChatRequest.java
      ChatResponse.java
      auth/
        LoginRequest.java
        LoginResponse.java
        MessageResponse.java
        RefreshTokenRequest.java
        RegisterRequest.java
        UserInfoResponse.java
      learning/
        LearningProfileRequest.java
        QuizGenerateRequest.java
        QuizSubmitRequest.java
        TutorRequest.java

    security/
      JwtAuthenticationEntryPoint.java
      JwtAuthenticationFilter.java
      JwtUtils.java
      UserDetailsImpl.java

    service/
      ArticleService.java
      AuthenticationService.java
      RefreshTokenService.java
      UserDetailsServiceImpl.java
      UserService.java
      ai/
        ArticleVerificationService.java
        CategoryTaggingService.java
        ChatService.java
        ClaimExtractionService.java
        RagContextService.java
        RateLimiter.java
        SynthesisService.java
        TimelineBuilderService.java
        VerificationService.java
        VisionAnalysisService.java
      external/
        ArticleScrapingService.java
        TavilySearchService.java
      feed/
        RssFeedMonitoringService.java
        RssFeedQueryService.java
        RssPreVerificationQueueService.java
        dto/
          RssFeedConfigDto.java
      learning/
        LearningService.java

    util/
      CacheMonitoringUtil.java

  src/main/resources/
    application.yaml
    db/migration/
      V1__initial_schema.sql
      V2__seed_data.sql
      V3__initial_rss_feeds.sql
      V4__learning_pivot_schema.sql

  docs/
    education-pivot-implementation.md

  Dockerfile
  docker-compose.yml
  pom.xml
```

## Data Model Summary

Verification/content domain:
- `articles`
- `claims`
- `evidence`
- `verifications`
- `synthesis`
- `synthesized_articles`
- `synthesized_article_claims`
- `rss_feed_configs`

User/auth domain:
- `users`
- `roles`
- `user_roles`
- `refresh_tokens`
- `saved_articles`
- `chat_sessions`
- `chat_messages`

Learning domain:
- `learner_profiles`
- `learning_quiz_sessions`
- `learning_quiz_questions`
- `learning_skill_progress`

## Education Outcomes This Enables

- Faster understanding of current developments.
- Higher trust in consumed content.
- Better exam framing through guided tutor responses.
- Continuous feedback loop from quiz -> skill progression.
- Personalized learning based on weaknesses and recent topics.

## Frontend Note

Frontend routes exist for:
- feed
- article details
- chat
- learning profile/feed/quiz/skills
- admin dashboards

This README keeps frontend details intentionally short because the primary value and evaluation depth are in backend architecture and services.
