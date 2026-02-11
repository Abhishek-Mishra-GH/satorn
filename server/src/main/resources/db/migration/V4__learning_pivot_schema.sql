-- Education pivot schema: learner profile, adaptive quiz, skill tracking

CREATE TABLE IF NOT EXISTS learner_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    exam_track VARCHAR(120),
    target_exam_date DATE,
    daily_study_minutes INTEGER,
    preferred_difficulty VARCHAR(30) DEFAULT 'INTERMEDIATE',
    weak_categories TEXT,
    strong_categories TEXT,
    learning_goals TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_learner_profile_user ON learner_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_learner_profile_exam_track ON learner_profiles(exam_track);

CREATE TABLE IF NOT EXISTS learning_quiz_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    focus_category VARCHAR(120),
    difficulty VARCHAR(30) DEFAULT 'INTERMEDIATE',
    status VARCHAR(30) DEFAULT 'GENERATED',
    total_questions INTEGER DEFAULT 0,
    correct_answers INTEGER DEFAULT 0,
    score_percent DOUBLE PRECISION DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_learning_quiz_user ON learning_quiz_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_learning_quiz_status ON learning_quiz_sessions(status);
CREATE INDEX IF NOT EXISTS idx_learning_quiz_created ON learning_quiz_sessions(created_at DESC);

CREATE TABLE IF NOT EXISTS learning_quiz_questions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES learning_quiz_sessions(id) ON DELETE CASCADE,
    synthesized_article_id BIGINT REFERENCES synthesized_articles(id) ON DELETE SET NULL,
    category VARCHAR(120),
    difficulty VARCHAR(30),
    question_text TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_option VARCHAR(1) NOT NULL,
    explanation TEXT,
    sort_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_learning_q_question_session ON learning_quiz_questions(session_id);
CREATE INDEX IF NOT EXISTS idx_learning_q_question_category ON learning_quiz_questions(category);

CREATE TABLE IF NOT EXISTS learning_skill_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_name VARCHAR(120) NOT NULL,
    mastery_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    attempted_questions INTEGER NOT NULL DEFAULT 0,
    correct_answers INTEGER NOT NULL DEFAULT 0,
    last_practiced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_learning_skill_user_category UNIQUE (user_id, category_name)
);

CREATE INDEX IF NOT EXISTS idx_learning_skill_user ON learning_skill_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_learning_skill_category ON learning_skill_progress(category_name);
CREATE INDEX IF NOT EXISTS idx_learning_skill_mastery ON learning_skill_progress(mastery_score);
