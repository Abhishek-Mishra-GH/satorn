-- SATORN v3.0 Database Schema
-- PostgreSQL with pgvector support

-- Enable pgvector extension for embeddings
CREATE EXTENSION IF NOT EXISTS vector;

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Roles table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- User-Role association
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Refresh tokens table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token TEXT UNIQUE NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Categories table
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    color VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Articles table
CREATE TABLE articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    url VARCHAR(2048) NOT NULL,
    image_url VARCHAR(500),
    author VARCHAR(100),
    domain VARCHAR(100),
    credibility_score DOUBLE PRECISION,
    status VARCHAR(50) DEFAULT 'PENDING',
    category_id BIGINT REFERENCES categories(id),
    submitted_by_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP,
    embedding vector(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT
);

-- Index for search and filtering
CREATE INDEX idx_articles_status ON articles(status);
CREATE INDEX idx_articles_category ON articles(category_id);
CREATE INDEX idx_articles_submitted_by ON articles(submitted_by_id);
CREATE INDEX idx_articles_submitted_at ON articles(submitted_at DESC);
CREATE INDEX idx_articles_embedding ON articles USING ivfflat (embedding vector_cosine_ops);

-- Claims table
CREATE TABLE claims (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    importance VARCHAR(50),
    claim_type VARCHAR(100),
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_claims_article ON claims(article_id);

-- Evidence table
CREATE TABLE evidence (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(500),
    title VARCHAR(255),
    snippet TEXT,
    source VARCHAR(100),
    relevance_score DOUBLE PRECISION,
    claim_id BIGINT NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evidence_claim ON evidence(claim_id);

-- Verifications table
CREATE TABLE verifications (
    id BIGSERIAL PRIMARY KEY,
    verdict VARCHAR(50),
    confidence DOUBLE PRECISION,
    reasoning TEXT,
    supporting_evidence TEXT,
    contradicting_evidence TEXT,
    article_id BIGINT REFERENCES articles(id) ON DELETE CASCADE,
    claim_id BIGINT REFERENCES claims(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_verifications_article ON verifications(article_id);
CREATE INDEX idx_verifications_claim ON verifications(claim_id);

-- Synthesis table (overall article analysis)
CREATE TABLE synthesis (
    id BIGSERIAL PRIMARY KEY,
    narrative TEXT,
    key_findings TEXT,
    credibility_score DOUBLE PRECISION,
    verdict VARCHAR(50),
    overall_confidence DOUBLE PRECISION,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_synthesis_article ON synthesis(article_id);

-- Chat sessions table
CREATE TABLE chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_sessions_user ON chat_sessions(user_id);

-- Chat messages table
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    response TEXT,
    intent VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_messages_session ON chat_messages(session_id);

-- Create audit log table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- RSS Feed Configuration table
CREATE TABLE rss_feed_configs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    feed_url VARCHAR(500) UNIQUE NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(100) NOT NULL,
    update_frequency_minutes INTEGER DEFAULT 60,
    last_checked TIMESTAMP,
    enabled BOOLEAN DEFAULT true,
    articles_processed BIGINT DEFAULT 0,
    last_error TEXT,
    consecutive_failures INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rss_enabled ON rss_feed_configs(enabled);
CREATE INDEX idx_rss_category ON rss_feed_configs(category);

-- Synthesized Articles table
CREATE TABLE synthesized_articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    original_content TEXT,
    synthesized_narrative TEXT,
    source_url VARCHAR(500),
    original_source VARCHAR(500),
    rss_feed_source VARCHAR(255),
    author VARCHAR(100),
    image_url VARCHAR(500),
    publish_date TIMESTAMP,
    credibility_score DOUBLE PRECISION,
    status VARCHAR(50) DEFAULT 'PENDING',
    verdict VARCHAR(50),
    category_id BIGINT REFERENCES categories(id),
    key_findings TEXT,
    timeline TEXT,
    claims_count INTEGER,
    verified_claims_count INTEGER,
    true_claims INTEGER,
    false_claims INTEGER,
    unverifiable_claims INTEGER,
    view_count BIGINT DEFAULT 0,
    is_trending BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_synthesized_status ON synthesized_articles(status);
CREATE INDEX idx_synthesized_category ON synthesized_articles(category_id);
CREATE INDEX idx_synthesized_created ON synthesized_articles(created_at DESC);
CREATE INDEX idx_synthesized_source_url ON synthesized_articles(source_url);

-- Synthesized Article Claims table
CREATE TABLE synthesized_article_claims (
    id BIGSERIAL PRIMARY KEY,
    synthesized_article_id BIGINT NOT NULL REFERENCES synthesized_articles(id) ON DELETE CASCADE,
    claim_text TEXT NOT NULL,
    claim_type VARCHAR(100),
    importance VARCHAR(50),
    verdict VARCHAR(50),
    confidence_score DOUBLE PRECISION,
    reasoning TEXT,
    supporting_evidence TEXT,
    contradicting_evidence TEXT
);

CREATE INDEX idx_syn_claim_article ON synthesized_article_claims(synthesized_article_id);
CREATE INDEX idx_syn_claim_verdict ON synthesized_article_claims(verdict);
