export type RssFeedConfigDto = {
  id: number;
  name: string;
  feedUrl: string;
  description?: string | null;
  category: string;
  updateFrequencyMinutes: number;
  lastChecked?: string | null;
  enabled: boolean;
  articlesProcessed: number;
  lastError?: string | null;
  consecutiveFailures: number;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type RssStatsDto = {
  totalFeeds: number;
  enabledFeeds: number;
  disabledFeeds: number;
  totalArticlesProcessed: number;
  feedsWithErrors: number;
};

export type RssQueueStatusDto = {
  queueSize: number;
  queueCapacity: number;
  processing: boolean;
  totalEnqueued: number;
  totalProcessed: number;
  totalDropped: number;
  totalRateLimited: number;
  rateLimiter: Record<string, { availableTokens: number; maxTokens: number; refillRate: number }>;
  manualRun: Record<string, unknown>;
};

export type RssManualRunStatusDto = {
  runId?: string;
  status?: "IDLE" | "RUNNING" | "COMPLETED" | "FAILED";
  stage?: string;
  startedAt?: string;
  finishedAt?: string;
  processed?: number;
  requeued?: number;
  dropped?: number;
  queueSize?: number;
  progressPercent?: number;
  message?: string;
  updatedAt?: string;
};

export type RssProbeDto = {
  feedId: number;
  feedName: string;
  feedUrl: string;
  status: "OK" | "ERROR";
  itemCount?: number;
  sampleItems?: { title: string; link: string; pubDate?: string | null }[];
  error?: string;
  durationMs: number;
};
