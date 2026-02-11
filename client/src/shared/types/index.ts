export interface User {
  id: number;
  username: string;
  email: string;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  id: number;
  username: string;
  email: string;
  roles: string[];
}

export interface Article {
  id: number;
  title: string;
  sourceUrl: string;
  rssFeedSource: string | null;
  author: string | null;
  credibilityScore: number | null;
  verdict: string | null;
  category: string | null;
  claimsCount: number | null;
  trueClaims: number | null;
  falseClaims: number | null;
  imageUrl: string | null;
  viewCount: number | null;
  isTrending: boolean | null;
  createdAt: string;
  
  // Optional frontend-specific or detail fields not in summary
  synthesizedNarrative?: string;
  keyFindings?: string;
  timeline?: string; // legacy field, prefer timelineEvents
  originalContent?: string;
  publishDate?: string;
  verifiedClaimsCount?: number;
  unverifiableClaims?: number;
  status?: string;
  savedAt?: string;
  
  // New Enhanced Fields
  keyFindingsBullets?: string[];
  timelineEvents?: { date: string | null; event: string }[];
  explainLikeIm5?: string;
  whyItMatters?: string;
  whatToWatch?: string[];
  keyFigures?: { value: string; context: string; sentiment?: string }[];
  quickStats?: {
    claimsAnalyzed: number;
    claimsVerified: number;
    trueClaims: number;
    falseClaims: number;
    unverifiableClaims: number;
    credibilityScore: number;
    confidenceLevel: string;
    trueRatio: number;
    falseRatio: number;
  };
  readingTimeMinutes?: number;
  discussionPrompts?: string[];
}

export interface ChatSession {
  id: number;
  title?: string;
  createdAt?: string;
  updatedAt?: string;
  active?: boolean;
}

export interface ChatMessage {
  id?: number;
  role: 'user' | 'assistant' | string;
  content: string;
  intent?: string;
  createdAt?: string;
  // Frontend only
  timestamp?: string;
  isStreaming?: boolean;
}

export interface ChatResponse {
  sessionId: number | null;
  userMessage: string;
  aiResponse: string;
  intent: string;
}

export interface SavedToggleResponse {
  message: string;
  saved: boolean;
}

export interface LikeToggleResponse {
  message: string;
  saved: boolean; // backend reuses "saved" field
}

// Admin RSS Monitoring
export interface MonitoringRun {
  runId: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
  stage: 'INITIALIZING' | 'ENQUEUEING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  startedAt: string;
  finishedAt?: string;
  durationSeconds?: number;
  requestedMaxArticles: number;
  maxArticles: number;
  processed: number;
  requeued: number;
  dropped: number;
  queueSize: number;
  progressPercent: number;
  updatedAt: string;
  error?: string;
}

export interface ProcessAllResponse {
  accepted: boolean;
  message: string;
  run: MonitoringRun;
}

export interface QueueStatus {
  queueSize: number;
  queueCapacity: number;
  processing: boolean;
  totalEnqueued: number;
  totalProcessed: number;
  totalDropped: number;
  totalRateLimited: number;
  rateLimiter: Record<string, unknown>;
  manualRun?: MonitoringRun;
}

export interface RssFeed {
  id: number;
  name: string;
  url: string;
  enabled: boolean;
  lastFetched?: string;
  status: 'IDLE' | 'PROCESSING' | 'FAILED';
  failureCount: number;
}
