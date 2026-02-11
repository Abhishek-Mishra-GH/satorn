# SATORN v3.0 - AI-Powered News Verification Platform

<div align="center">

![SATORN Logo](https://img.shields.io/badge/SATORN-v3.0-blue?style=for-the-badge)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen?style=flat-square&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M1-orange?style=flat-square)](https://spring.io/projects/spring-ai)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](https://opensource.org/licenses/MIT)

**An intelligent, multi-agent AI system for automated news verification, fact-checking, and credibility analysis.**

[Features](#-features) • [Architecture](#-architecture) • [Quick Start](#-quick-start) • [Documentation](#-documentation) • [API Reference](#-api-reference) • [Contributing](#-contributing)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Technology Stack](#-technology-stack)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [AI Agents](#-ai-agents)
- [RSS Pipeline](#-rss-monitoring-pipeline)
- [Security](#-security)
- [Monitoring](#-monitoring)
- [License](#-license)

---

## 🌟 Overview

SATORN (Smart Analysis & Trustworthiness Of Real News) is a production-ready AI-powered platform that automatically verifies news articles, extracts factual claims, searches for supporting evidence, and generates comprehensive credibility reports. It combines multiple AI agents with advanced NLP, web scraping, and vector search capabilities to combat misinformation.

### Why SATORN?

- 🤖 **Multi-Agent AI Architecture**: 9 specialized AI agents working in concert
- 📊 **Automated Fact-Checking**: Extract claims and verify against credible sources
- 💬 **Conversational Interface**: Natural language chat with smart intent detection
- 📰 **RSS Monitoring**: Automated ingestion and verification of news feeds
- 🔍 **Semantic Search**: RAG-powered article discovery with pgvector
- 🛡️ **Enterprise Security**: JWT authentication with role-based access control
- 📈 **Production Ready**: Docker support, monitoring, caching, and horizontal scaling

---

## 🎯 Key Features

### Core Verification System

- **Automated Article Analysis**
  - Web scraping and content extraction
  - Claim identification and classification
  - Multi-source evidence gathering
  - Credibility scoring (0-100 scale)
  - Timeline reconstruction

- **AI-Powered Verification Pipeline**
  - Smart claim extraction with importance ranking
  - Fact-checking against Tavily Search API
  - Cross-reference with multiple LLM providers (OpenAI, Groq)
  - Synthesis of findings into human-readable reports

- **Advanced Search & Discovery**
  - RAG (Retrieval-Augmented Generation) with pgvector
  - Semantic similarity search
  - Trending topics detection
  - Category-based filtering

### User Interface

- **Unified Chat Interface**
  - Natural language article submission
  - Real-time verification status
  - SSE (Server-Sent Events) streaming
  - Multi-turn conversations with context

- **REST API**
  - OpenAPI 3.0 specification
  - Swagger UI for interactive testing
  - JWT bearer token authentication
  - Comprehensive error handling

### RSS Automation

- **Intelligent Feed Monitoring**
  - Multi-feed discovery and parsing
  - Priority-based article ranking
  - Deduplication and cooldown
  - Rate-limit aware processing

- **Queue Management**
  - Redis-backed job queue
  - Batch processing optimization
  - Admin controls for queue operations
  - Real-time monitoring dashboard

### Enterprise Features

- **Authentication & Authorization**
  - JWT access + refresh tokens
  - Role-based access control (ADMIN, MODERATOR, USER)
  - Account lockout after failed attempts
  - Token revocation support

- **Performance & Scalability**
  - Redis caching for hot endpoints
  - Connection pooling (HikariCP)
  - Async processing with thread pools
  - Horizontal scaling ready

- **Monitoring & Observability**
  - Spring Boot Actuator integration
  - Health checks and metrics
  - Prometheus-compatible endpoints
  - Structured logging

---

## 🏗️ System Architecture


### Verification Pipeline Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                    ARTICLE VERIFICATION PIPELINE                     │
└──────────────────────────────────────────────────────────────────────┘

1. INGESTION
   ┌─────────────┐
   │  URL Input  │
   │   (User)    │
   └──────┬──────┘
          │
          ▼
   ┌─────────────────┐
   │  Ingest Agent   │  → Web scraping, content extraction
   │  (Jsoup)        │     Metadata parsing, author detection
   └──────┬──────────┘
          │
          ▼

2. CLAIM EXTRACTION
   ┌───────────────────────┐
   │ Claim Extract Agent   │  → Identify factual claims
   │ (GPT-4/Groq)          │     Classify claim types (FACTUAL, STATISTICAL, QUOTE)
   └───────┬───────────────┘     Rank importance (HIGH, MEDIUM, LOW)
           │
           ▼

3. VERIFICATION
   ┌──────────────────────┐
   │ Verification Agent   │  → Search evidence (Tavily API)
   │ (Multi-source)       │     Cross-reference facts
   └───────┬──────────────┘     Generate verdict + confidence
           │
           ▼               
           │                     Verdict Options:
           ├─────────────────►  ✓ TRUE (high confidence)
           ├─────────────────►  ✗ FALSE (contradicted)
           ├─────────────────►  ⚠ MISLEADING (context missing)
           ├─────────────────►  ? UNVERIFIABLE (insufficient evidence)
           └─────────────────►  ◐ PARTIALLY_TRUE (mixed evidence)
           │
           ▼

4. SYNTHESIS
   ┌──────────────────────┐
   │  Synthesis Agent     │  → Aggregate all verifications
   │  (GPT-4)             │     Calculate credibility score (0-100)
   └───────┬──────────────┘     Generate summary report
           │
           ▼

5. CATEGORIZATION
   ┌──────────────────────┐
   │  Category Agent      │  → Classify topic (Politics, Health, Tech, etc.)
   │  (Zero-shot)         │     Tag for discovery
   └───────┬──────────────┘
           │
           ▼

6. STORAGE & INDEXING
   ┌──────────────────────┐
   │  Database + Vector   │  → Store in PostgreSQL
   │  (pgvector)          │     Index for semantic search
   └───────┬──────────────┘     Cache hot results
           │
           ▼
   ┌──────────────────────┐
   │   Result Ready       │  → Notify user
   │   (User Dashboard)   │     API response with full report
   └──────────────────────┘

   Processing Time: 45-120 seconds (typical)
```

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT APPLICATIONS                         │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐             │
│  │  Web Client  │   │ Mobile App   │   │  Admin UI    │             │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘             │
└─────────┼──────────────────┼──────────────────┼─────────────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │ HTTPS/REST API
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SpringBoot Application                         │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │               Security Layer (JWT Auth)                        │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐             │
│  │  Article     │   │     Chat     │   │     RSS      │             │
│  │  Controller  │   │  Controller  │   │  Controller  │             │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘             │
└─────────┼──────────────────┼──────────────────┼─────────────────────┘
          │                  │                  │
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER (Business Logic)                 │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                   AI AGENT ORCHESTRATION                       │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │ │
│  │  │  Router  │  │  Ingest  │  │  Claim   │  │  Verify  │        │ │
│  │  │  Agent   │  │  Agent   │  │  Extract │  │  Agent   │        │ │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │ │
│  │  │Synthesis │  │ Category │  │   Chat   │  │   News   │        │ │
│  │  │  Agent   │  │  Agent   │  │  Agent   │  │  Search  │        │ │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐    │
│  │ Article         │  │  Verification    │  │  RSS             │    │
│  │ Service         │  │  Pipeline        │  │  Monitoring      │    │
│  └─────────────────┘  └──────────────────┘  └──────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
          │                  │                  │
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         DATA LAYER                                  │
│                                                                     │
│  ┌──────────────────────┐  ┌──────────────────────┐                 │
│  │   PostgreSQL 15      │  │     Redis 7          │                 │
│  │  ┌────────────────┐  │  │  ┌────────────────┐  │                 │
│  │  │   Articles     │  │  │  │   Cache        │  │                 │
│  │  │   Claims       │  │  │  │   Queue        │  │                 │
│  │  │   Evidence     │  │  │  │   Sessions     │  │                 │
│  │  │   Users        │  │  │  │   Rate Limits  │  │                 │
│  │  │   pgvector     │  │  │  └────────────────┘  │                 │
│  │  └────────────────┘  │  └──────────────────────┘                 │
│  └──────────────────────┘                                           │
└─────────────────────────────────────────────────────────────────────┘
          │                  │
          ▼                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    EXTERNAL SERVICES                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │   OpenAI     │  │    Groq      │  │    Tavily    │               │
│  │   GPT-4      │  │  LLaMA 3.1   │  │    Search    │               │
│  └──────────────┘  └──────────────┘  └──────────────┘               │
└─────────────────────────────────────────────────────────────────────┘
```
### Ask SATORN AI Agent Interaction Diagram

```
┌────────────────────────────────────────────────────────────────────┐
│                        SMART ROUTER AGENT                          │
│              (Intent Detection & Request Routing)                  │
└───────────┬────────────────────────────────────────────────────────┘
            │
            │ Detect Intent
            │
            ├────────────► ARTICLE_VERIFICATION
            │              ┌──────────────────────┐
            │              │   Ingest Agent       │
            │              │   Claim Extract      │
            │              │   Verification       │
            │              │   Synthesis          │
            │              └──────────────────────┘
            │
            ├────────────► NEWS_SEARCH
            │              ┌──────────────────────┐
            │              │   News Search Agent  │
            │              │   (Tavily API)       │
            │              └──────────────────────┘
            │
            ├────────────► GENERAL_CHAT
            │              ┌──────────────────────┐
            │              │   Chat Agent         │
            │              │   (Conversational)   │
            │              └──────────────────────┘
            │
            └────────────► HELP
                           ┌──────────────────────┐
                           │   Static Response    │
                           │   (Documentation)    │
                           └──────────────────────┘
```
---

## 🛠️ Technology Stack

### Backend Framework
- **Spring Boot 3.2.1** - Core application framework
- **Spring Security 6.2.1** - JWT authentication & authorization
- **Spring AI 1.0.0-M1** - AI agent orchestration
- **Spring Data JPA** - Data persistence layer

### Databases & Caching
- **PostgreSQL 15** - Primary database with pgvector extension
- **Redis 7** - Caching, queue management, session storage

### AI & LLM Providers
- **OpenAI GPT-4** - Primary LLM for verification and synthesis
- **Groq (LLaMA 3.1)** - Fast inference for claim extraction
- **Tavily Search API** - Real-time web evidence gathering

### Libraries & Tools
- **Jsoup** - Web scraping and HTML parsing
- **Lombok** - Boilerplate code reduction
- **Jackson** - JSON serialization/deserialization
- **HikariCP** - JDBC connection pooling
- **Flyway** - Database migration management
- **Apache HttpClient 5** - HTTP communication

### DevOps & Deployment
- **Docker & Docker Compose** - Containerization
- **Maven** - Build and dependency management
- **Spring Boot Actuator** - Monitoring and metrics
- **Swagger/OpenAPI 3.0** - API documentation

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **Docker & Docker Compose** (for infrastructure)
- **PostgreSQL 15** (if running locally)
- **Redis 7** (optional, for caching)

### API Keys Required

You'll need API keys from:
- [OpenAI](https://platform.openai.com/) - GPT-4 access
- [Groq](https://groq.com/) - LLaMA inference
- [Tavily](https://tavily.com/) - Search API

### Quick Start (Docker)

#### Option 1: Infrastructure Only (Recommended for Development)

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/satorn-v3.git
cd satorn-v3

# 2. Create .env file
cp .env.example .env
# Edit .env with your API keys

# 3. Start PostgreSQL + Redis
docker-compose up -d

# 4. Run application locally
./mvnw spring-boot:run
```

#### Option 2: Full Stack (Application + Infrastructure)

```bash
# Build and run everything
docker-compose --profile fullstack up --build -d

# View logs
docker-compose logs -f satorn-app

# Stop all services
docker-compose down
```

### Manual Setup (No Docker)

```bash
# 1. Install PostgreSQL 15
# Create database
createdb satorn_db
createuser satorn_user

# 2. Install Redis (optional)
# Or skip and disable Redis in application.yml

# 3. Configure application
cp src/main/resources/application-dev.yml src/main/resources/application.yml
# Edit database connection and API keys

# 4. Run database migrations
./mvnw flyway:migrate

# 5. Build application
./mvnw clean package -DskipTests

# 6. Run application
java -jar target/satorn-3.0.0.jar
```

### Verify Installation

```bash
# Health check
curl http://localhost:8080/actuator/health

# Should return:
# {"status":"UP","components":{"db":{"status":"UP"}}}

# Access Swagger UI
open http://localhost:8080/swagger-ui/index.html
```

---

## ⚙️ Configuration

### Environment Variables

Create `.env` file in project root:

```bash
# ============================================
# DATABASE CONFIGURATION
# ============================================
DB_HOST=localhost
DB_PORT=5432
DB_NAME=satorn_db
DB_USERNAME=satorn_user
DB_PASSWORD=your_secure_password

# ============================================
# REDIS CONFIGURATION
# ============================================
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_TIMEOUT=3000

# ============================================
# SECURITY & JWT
# ============================================
JWT_SECRET=your_very_long_secret_key_minimum_32_characters_required
JWT_EXPIRATION=86400000         # 24 hours in milliseconds
JWT_REFRESH_EXPIRATION=604800000 # 7 days in milliseconds

# ============================================
# AI SERVICE API KEYS
# ============================================
OPENAI_API_KEY=sk-...
GROQ_API_KEY=gsk_...
TAVILY_API_KEY=tvly-...

# ============================================
# RSS MONITORING CONFIGURATION
# ============================================
RSS_MONITORING_ENABLED=true
RSS_MONITORING_INTERVAL=300000  # 5 minutes
RSS_ENQUEUE_LIMIT=10            # Max articles per feed per scan
RSS_BATCH_SIZE=5                # Process 5 articles per batch
RSS_MAX_CLAIMS=10               # Max claims to extract per article

# ============================================
# APPLICATION SETTINGS
# ============================================
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev      # dev, prod
LOG_LEVEL=INFO                   # DEBUG, INFO, WARN, ERROR

# ============================================
# OPTIONAL: OBSERVABILITY
# ============================================
MANAGEMENT_ENDPOINTS_ENABLED=true
ACTUATOR_METRICS_EXPORT_PROMETHEUS_ENABLED=true
```

### Application Configuration

Edit `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: SATORN
  
  # Database
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:satorn_db}
    username: ${DB_USERNAME:satorn_user}
    password: ${DB_PASSWORD:satorn_pass}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
  
  # JPA
  jpa:
    hibernate:
      ddl-auto: validate  # Use 'update' for dev, 'validate' for prod
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
  
  # Redis
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: ${REDIS_TIMEOUT:3000}
  
  # Spring AI
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.7
    vectorstore:
      pgvector:
        initialize-schema: true
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536

# JWT Configuration
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400000}
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}

# External APIs
tavily:
  api-key: ${TAVILY_API_KEY}
  base-url: https://api.tavily.com

groq:
  api-key: ${GROQ_API_KEY}
  base-url: https://api.groq.com/openai/v1

# RSS Monitoring
rss:
  monitoring:
    enabled: ${RSS_MONITORING_ENABLED:true}
    interval: ${RSS_MONITORING_INTERVAL:300000}
    enqueue-limit-per-feed: ${RSS_ENQUEUE_LIMIT:10}
    process-batch-size: ${RSS_BATCH_SIZE:5}
    max-claims-per-article: ${RSS_MAX_CLAIMS:10}

# Server
server:
  port: ${SERVER_PORT:8080}
  error:
    include-message: always
    include-binding-errors: always

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: ${ACTUATOR_METRICS_EXPORT_PROMETHEUS_ENABLED:true}

# Logging
logging:
  level:
    root: ${LOG_LEVEL:INFO}
    com._xdev.satorn: DEBUG
    org.springframework.security: DEBUG
```

---

## 📚 API Documentation

### Swagger UI

Access interactive API documentation:
```
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI Specification

Download the OpenAPI JSON:
```
http://localhost:8080/v3/api-docs
```

### Authentication

All protected endpoints require JWT Bearer token:

```bash
# 1. Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# 2. Login and get token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "SecurePass123!"
  }'

# Response:
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "refreshToken": "refresh_token_here",
#   "tokenType": "Bearer",
#   "expiresIn": 86400
# }

# 3. Use token in subsequent requests
TOKEN="your_access_token_here"

curl -X POST http://localhost:8080/api/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://example.com/news-article"
  }'
```

### Key Endpoints

#### Authentication & User Management

```bash
POST   /api/auth/register      # Register new user
POST   /api/auth/login          # Login and get JWT
POST   /api/auth/refresh        # Refresh access token
POST   /api/auth/logout         # Logout (revoke token)
GET    /api/users/me            # Get current user profile
PUT    /api/users/me            # Update user profile
```

#### Article Verification

```bash
POST   /api/articles            # Submit article for verification
GET    /api/articles/{id}       # Get article details with verification
GET    /api/articles            # List all articles (paginated)
GET    /api/articles/my         # Get current user's articles
DELETE /api/articles/{id}       # Delete article (owner or admin)
POST   /api/articles/{id}/reprocess  # Re-verify article
```

#### Synthesized Articles (Verified Content)

```bash
GET    /api/synthesized          # Browse verified articles
GET    /api/synthesized/{id}     # Get synthesis report
GET    /api/synthesized/search   # Semantic search (RAG)
GET    /api/synthesized/trending # Trending topics
GET    /api/synthesized/top      # Top credible articles
```

#### Chat Interface

```bash
POST   /api/chat                # Send message (unified endpoint)
POST   /api/chat/stream         # Chat with SSE streaming
GET    /api/chat/sessions       # Get user's chat sessions
GET    /api/chat/sessions/{id}  # Get session history
```

#### Learning (Education Pivot)

```bash
GET    /api/learning/profile           # Get learner profile
PUT    /api/learning/profile           # Create/update learner profile
GET    /api/learning/recommendations   # Personalized current-affairs feed
POST   /api/learning/quiz/generate     # Generate adaptive quiz
POST   /api/learning/quiz/submit       # Submit answers and score quiz
GET    /api/learning/skills            # Category skill mastery tracker
POST   /api/learning/tutor             # AI tutor response with study context
```

#### Search

```bash
POST   /api/search/news         # Search news via Tavily
POST   /api/search/verify       # Search for verification evidence
```

#### RSS Management (Admin Only)

```bash
GET    /api/rss/feeds           # List RSS feeds
POST   /api/rss/feeds           # Add new RSS feed
PUT    /api/rss/feeds/{id}      # Update RSS feed
DELETE /api/rss/feeds/{id}      # Delete RSS feed
POST   /api/rss/probe           # Test RSS feed URL
GET    /api/rss/queue/stats     # Queue statistics
POST   /api/rss/queue/process   # Manually trigger processing
POST   /api/rss/queue/clear     # Clear queue (admin only)
```

### Example: Complete Workflow

```bash
# Set your token
export TOKEN="your_jwt_token"

# 1. Submit article for verification
ARTICLE_ID=$(curl -s -X POST http://localhost:8080/api/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.bbc.com/news/example"}' \
  | jq -r '.id')

echo "Article submitted: $ARTICLE_ID"

# 2. Wait for processing (typically 45-120 seconds)
sleep 60

# 3. Check verification status
curl -s -X GET "http://localhost:8080/api/articles/$ARTICLE_ID" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.status'

# 4. Get full synthesis report
curl -s -X GET "http://localhost:8080/api/articles/$ARTICLE_ID" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.synthesis'

# 5. Search for similar articles
curl -s -X GET "http://localhost:8080/api/synthesized/search?query=climate+change" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.content[].title'
```

---

## 🤖 AI Agents

SATORN uses 9 specialized AI agents orchestrated through Spring AI:

### 1. Smart Router Agent
- **Purpose**: Intent detection and request routing
- **Model**: GPT-4 (zero-shot classification)
- **Intents**: ARTICLE_VERIFICATION, NEWS_SEARCH, IMAGE_ANALYSIS, GENERAL_CHAT, HELP
- **Accuracy**: ~95% intent detection

### 2. Ingest Agent
- **Purpose**: Web scraping and content extraction
- **Technology**: Jsoup + custom parsing
- **Capabilities**: 
  - HTML parsing and cleaning
  - Metadata extraction (author, date, publisher)
  - Main content isolation (article-specific algorithms)
  - Image and media handling

### 3. Claim Extraction Agent
- **Purpose**: Identify factual claims in articles
- **Model**: GPT-4 or Groq (LLaMA 3.1)
- **Output**: 
  - Claim text
  - Type (FACTUAL, STATISTICAL, QUOTE, OPINION)
  - Importance (HIGH, MEDIUM, LOW)
  - Context window

### 4. Verification Agent
- **Purpose**: Fact-check claims against evidence
- **Model**: GPT-4 with function calling
- **Process**:
  1. Search Tavily API for evidence
  2. Cross-reference multiple sources
  3. Generate verdict: TRUE, FALSE, MISLEADING, UNVERIFIABLE, PARTIALLY_TRUE
  4. Calculate confidence score (0.0-1.0)
  5. Provide explanation with citations

### 5. Synthesis Agent
- **Purpose**: Aggregate verification results
- **Model**: GPT-4
- **Output**:
  - Overall verdict
  - Credibility score (0-100)
  - Summary paragraph
  - Key findings list
  - Recommendations

### 6. Category Tagging Agent
- **Purpose**: Classify article topics
- **Model**: GPT-4 (zero-shot classification)
- **Categories**: Politics, Health, Technology, Science, Business, Environment, Sports, Entertainment, World, Local

### 7. Chat Agent
- **Purpose**: Conversational interface
- **Model**: GPT-4 with conversation history
- **Features**:
  - Multi-turn conversations
  - Context awareness
  - Fact-checking explanations
  - Media literacy guidance

### 8. News Search Agent
- **Purpose**: Discover news articles
- **Technology**: Tavily Search API wrapper
- **Capabilities**:
  - News-specific search
  - Source ranking
  - Relevance scoring
  - Snippet extraction

### 9. Vision Agent
- **Purpose**: Extract text from images
- **Model**: GPT-4 Vision
- **Use Cases**:
  - Screenshot verification
  - Meme fact-checking
  - Document OCR

---

## 📡 RSS Monitoring Pipeline

### How It Works

1. **Discovery Phase** (Every 5 minutes)
   - Fetch all active RSS feeds from database
   - Parse RSS/Atom XML
   - Extract articles with metadata

2. **Scoring Phase**
   - Calculate recency score (newer = higher)
   - Apply keyword matching (configurable priority terms)
   - Factor in engagement signals (if available)

3. **Deduplication & Cooldown**
   - Check if article already exists (URL hash)
   - Apply per-source cooldown (avoid spam)
   - Filter out low-quality sources

4. **Enqueueing** (Top N per feed)
   - Sort by score
   - Select top N articles (default: 10)
   - Add to Redis queue with priority
   - Respect rate limits

5. **Background Processing** (Async batch)
   - Dequeue batch (default: 5 articles)
   - Run full verification pipeline
   - Store results in PostgreSQL
   - Index embeddings to pgvector

6. **Monitoring & Stats**
   - Track processing times
   - Monitor queue depth
   - Log errors and retries
   - Expose metrics via Actuator

### Configuration

```yaml
rss:
  monitoring:
    enabled: true
    interval: 300000                    # 5 minutes
    enqueue-limit-per-feed: 10          # Max articles per scan
    process-batch-size: 5               # Concurrent processing
    max-claims-per-article: 10          # Claim extraction limit
    cooldown-period: 3600000            # 1 hour per source
    score-threshold: 0.3                # Minimum score to enqueue
```

### Admin Operations

```bash
# Get queue statistics
curl http://localhost:8080/api/rss/queue/stats \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Response:
# {
#   "queueSize": 45,
#   "processing": 5,
#   "completed": 1234,
#   "failed": 12,
#   "averageProcessingTime": 67.3
# }

# Manually trigger processing
curl -X POST http://localhost:8080/api/rss/queue/process \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Clear queue (careful!)
curl -X POST http://localhost:8080/api/rss/queue/clear \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 🔒 Security

### Authentication Flow

```
1. User registers or logs in
2. Server validates credentials
3. Server generates JWT access token (24h) + refresh token (7d)
4. Client stores tokens securely
5. Client includes access token in Authorization header
6. Server validates token on each request
7. On expiry, client uses refresh token to get new access token
8. On logout, server revokes refresh token
```

### Role-Based Access Control (RBAC)

| Role | Permissions |
|------|-------------|
| **USER** | Submit articles, view own submissions, use chat, search |
| **MODERATOR** | All USER permissions + view all articles, flag content |
| **ADMIN** | All MODERATOR permissions + manage users, RSS feeds, system config |

### Security Best Practices

✅ **Implemented:**
- Password hashing with BCrypt (strength: 10)
- JWT with RS256 signing algorithm
- Refresh token rotation
- Account lockout after 5 failed login attempts
- CORS configuration
- SQL injection prevention (JPA/Hibernate)
- XSS protection (input sanitization)
- Rate limiting on auth endpoints
---

## 🚢 Deployment

### Docker Production Deployment

```bash
# 1. Build production image
docker build -t satorn:3.0.0 -f Dockerfile.prod .

# 2. Run with docker-compose (production profile)
docker-compose -f docker-compose.prod.yml up -d

# 3. Monitor logs
docker-compose logs -f satorn-app

# 4. Scale horizontally
docker-compose up --scale satorn-app=3
```

---

## 📊 Monitoring

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Redis health
curl http://localhost:8080/actuator/health/redis

# Detailed health (requires authentication)
curl http://localhost:8080/actuator/health \
  -H "Authorization: Bearer $TOKEN"
```

### Metrics

```bash
# All metrics
curl http://localhost:8080/actuator/metrics

# JVM memory usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP requests
curl http://localhost:8080/actuator/metrics/http.server.requests

# Custom metrics
curl http://localhost:8080/actuator/metrics/articles.verified.total
```

### Prometheus Integration

Add to `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'satorn'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### Grafana Dashboard

Import dashboard JSON from `monitoring/grafana-dashboard.json`:

- Application metrics
- JVM statistics
- API response times
- Queue depth and processing rate
- Database connection pool
- Error rates

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Spring team for Spring Boot and Spring AI frameworks
- OpenAI for GPT-4 API
- Groq for fast LLM inference
- Tavily for search API
- PostgreSQL team for pgvector extension
- Open source community

---

## 📞 Support & Contact

- **Documentation**: [https://docs.satorn.ai](https://docs.satorn.ai)
- **Issues**: [GitHub Issues](https://github.com/yourusername/satorn-v3/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/satorn-v3/discussions)
- **Email**: support@satorn.ai
- **Twitter**: [@SatornAI](https://twitter.com/SatornAI)

---

<div align="center">

[⬆ Back to Top](#satorn-v30---ai-powered-news-verification-platform)

</div>
