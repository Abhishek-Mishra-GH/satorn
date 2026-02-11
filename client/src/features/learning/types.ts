export type DifficultyLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

export interface LearnerProfile {
  examTrack: string;
  targetExamDate: string;
  dailyStudyMinutes: number;
  preferredDifficulty: DifficultyLevel;
  weakCategories: string[];
  strongCategories: string[];
  learningGoals: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateProfileRequest {
  examTrack?: string;
  targetExamDate?: string;
  dailyStudyMinutes?: number;
  preferredDifficulty?: DifficultyLevel;
  weakCategories?: string[];
  strongCategories?: string[];
  learningGoals?: string;
}

export interface LearningArticle {
  id: number;
  title: string;
  category: string;
  verdict: string;
  credibilityScore: number;
  createdAt: string;
  sourceUrl: string;
  score?: number;
  whyRecommended?: string[];
}

export interface RecommendationsResponse {
  total: number;
  page: number;
  size: number;
  totalPages: number;
  articles: LearningArticle[];
}

export interface QuizGenerateRequest {
  questionCount?: number;
  category?: string;
  difficulty?: DifficultyLevel;
}

export interface QuizQuestion {
  id: number;
  question: string;
  category: string;
  difficulty: DifficultyLevel;
  sourceArticleId: number;
  options: Record<string, string>; // e.g., { "A": "...", "B": "..." }
}

export interface QuizSession {
  quizSessionId: number;
  status: 'GENERATED' | 'SUBMITTED' | 'ABANDONED';
  difficulty: DifficultyLevel;
  focusCategory: string;
  questionCount: number;
  estimatedTimeMinutes: number;
  questions: QuizQuestion[];
}

export interface QuizAnswer {
  questionId: number;
  selectedOption: string;
}

export interface QuizSubmitRequest {
  quizSessionId: number;
  answers: QuizAnswer[];
}

export interface QuestionResult {
  questionId: number;
  question: string;
  selectedOption: string;
  correctOption: string;
  isCorrect: boolean;
  explanation: string;
  category: string;
}

export interface SkillUpdate {
  category: string;
  masteryScore: number;
  attemptedQuestions: number;
  correctAnswers: number;
  accuracyPercent: number;
  lastPracticedAt: string;
}

export interface QuizResult {
  quizSessionId: number;
  status: string;
  scorePercent: number;
  correctAnswers: number;
  totalQuestions: number;
  skillsUpdated: SkillUpdate[];
  results: QuestionResult[];
}

export interface SkillStats {
  category: string;
  masteryScore: number;
  attemptedQuestions: number;
  correctAnswers: number;
  accuracyPercent: number;
  lastPracticedAt: string;
}

export interface SkillsResponse {
  overallMastery: number;
  totalAttemptedQuestions: number;
  totalCorrectAnswers: number;
  needsFocusCategory: string;
  strongestCategory: string;
  skills: SkillStats[];
}

export interface TutorRequest {
  question: string;
  contextArticleId?: number;
}

export interface TutorResponse {
  answer: string;
  question: string;
  nextActions: string[];
  contextArticles: LearningArticle[];
}
