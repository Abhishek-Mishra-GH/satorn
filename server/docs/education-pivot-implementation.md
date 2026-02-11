# Education Pivot Backend Implementation
This document outlines the implementation details of the backend services supporting the education pivot features in Satorn. It covers the API contract, data models, and key service components.
## API Contract (`/api/learning`)

All endpoints require JWT authentication (`ROLE_USER` or `ROLE_ADMIN`).

### `GET /api/learning/profile`

Response `200`:

```json
{
  "examTrack": "General Current Affairs",
  "targetExamDate": "2026-12-01",
  "dailyStudyMinutes": 45,
  "preferredDifficulty": "INTERMEDIATE",
  "weakCategories": ["Economy", "Environment"],
  "strongCategories": ["Technology"],
  "learningGoals": "Improve answer-writing quality",
  "createdAt": "2026-02-11T16:20:01.123",
  "updatedAt": "2026-02-11T16:20:01.123"
}
```

### `PUT /api/learning/profile`

Request body:

```json
{
  "examTrack": "UPSC CSE",
  "targetExamDate": "2026-12-01",
  "dailyStudyMinutes": 90,
  "preferredDifficulty": "ADVANCED",
  "weakCategories": ["Economy", "Environment"],
  "strongCategories": ["Technology", "Politics"],
  "learningGoals": "GS2 + GS3 current affairs mastery"
}
```

Response `200`: same shape as `GET /profile`.

### `GET /api/learning/recommendations?page=0&size=10`

Response `200`:

```json
{
  "total": 125,
  "page": 0,
  "size": 10,
  "totalPages": 13,
  "articles": [
    {
      "id": 42,
      "title": "Example article",
      "category": "Economy",
      "verdict": "MOSTLY_TRUE",
      "credibilityScore": 84.0,
      "createdAt": "2026-02-11T10:25:00",
      "sourceUrl": "https://...",
      "score": 92.4,
      "whyRecommended": [
        "High credibility verification",
        "Targets your weak area: Economy"
      ]
    }
  ]
}
```

### `POST /api/learning/quiz/generate`

Request body (all optional):

```json
{
  "questionCount": 6,
  "category": "Economy",
  "difficulty": "INTERMEDIATE"
}
```

Behavior:
- Primary path: fetches latest current-affairs sources from Tavily (`topic=news`) using the requested category/topic and learner profile.
- Then AI generates exam-oriented MCQs from those sources.
- Fallback path: if Tavily/AI generation fails, falls back to internal verified-article quiz generation.

Response `200`:

```json
{
  "quizSessionId": 11,
  "status": "GENERATED",
  "difficulty": "INTERMEDIATE",
  "focusCategory": "Economy",
  "questionCount": 6,
  "estimatedTimeMinutes": 12,
  "generationStrategy": "TAVILY_NEWS_AI",
  "sourceQuery": "latest Economy current affairs updates ...",
  "sources": [
    {
      "title": "Recent policy update ...",
      "url": "https://...",
      "score": 0.91,
      "snippet": "..."
    }
  ],
  "questions": [
    {
      "id": 101,
      "question": "Which policy implication is most likely from ...?",
      "category": "Economy",
      "difficulty": "INTERMEDIATE",
      "sourceArticleId": null,
      "options": {
        "A": "...",
        "B": "...",
        "C": "...",
        "D": "..."
      }
    }
  ]
}
```

### `POST /api/learning/quiz/submit`

Request body:

```json
{
  "quizSessionId": 11,
  "answers": [
    { "questionId": 101, "selectedOption": "A" },
    { "questionId": 102, "selectedOption": "C" }
  ]
}
```

Response `200`:

```json
{
  "quizSessionId": 11,
  "status": "SUBMITTED",
  "scorePercent": 66.67,
  "correctAnswers": 4,
  "totalQuestions": 6,
  "skillsUpdated": [
    {
      "category": "Economy",
      "masteryScore": 58.33,
      "attemptedQuestions": 12,
      "correctAnswers": 7,
      "accuracyPercent": 58.33,
      "lastPracticedAt": "2026-02-11T16:30:05.123"
    }
  ],
  "results": [
    {
      "questionId": 101,
      "question": "...",
      "selectedOption": "A",
      "correctOption": "A",
      "isCorrect": true,
      "explanation": "...",
      "category": "Economy"
    }
  ]
}
```

### `GET /api/learning/skills`

Response `200`:

```json
{
  "overallMastery": 61.11,
  "totalAttemptedQuestions": 36,
  "totalCorrectAnswers": 22,
  "needsFocusCategory": "Environment",
  "strongestCategory": "Technology",
  "skills": [
    {
      "category": "Environment",
      "masteryScore": 45.0,
      "attemptedQuestions": 10,
      "correctAnswers": 4,
      "accuracyPercent": 40.0,
      "lastPracticedAt": "2026-02-11T16:30:05.123"
    }
  ]
}
```

### `POST /api/learning/tutor`

Request body:

```json
{
  "question": "Explain the economic impact of recent inflation data for UPSC mains.",
  "contextArticleId": 42
}
```

Response `200`:

```json
{
  "answer": "Tutor response markdown/plain text...",
  "question": "Explain the economic impact ...",
  "nextActions": [
    "Attempt a quiz on your weakest category from /api/learning/quiz/generate",
    "Read one recommendation deeply and summarize it in 5 bullet points",
    "Revisit this tutor with a follow-up question on policy implications"
  ],
  "contextArticles": [
    {
      "id": 42,
      "title": "Example article",
      "category": "Economy",
      "verdict": "MOSTLY_TRUE",
      "credibilityScore": 84.0,
      "sourceUrl": "https://..."
    }
  ]
}
```
