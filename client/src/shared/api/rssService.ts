import api from './client';
import { 
  RssFeedConfigDto, 
  RssStatsDto, 
  RssQueueStatusDto, 
  RssManualRunStatusDto, 
  RssProbeDto 
} from '../types/rssAdmin';

const BASE_URL = '/api/admin/rss-feeds';

export const rssService = {
  // --- Feed Management ---
  getFeeds: async () => {
    const response = await api.get<RssFeedConfigDto[]>(BASE_URL);
    return response.data;
  },

  getFeed: async (id: number) => {
    const response = await api.get<RssFeedConfigDto>(`${BASE_URL}/${id}`);
    return response.data;
  },

  createFeed: async (data: Partial<RssFeedConfigDto>) => {
    const response = await api.post<RssFeedConfigDto>(BASE_URL, data);
    return response.data;
  },

  updateFeed: async (id: number, data: Partial<RssFeedConfigDto>) => {
    const response = await api.put<RssFeedConfigDto>(`${BASE_URL}/${id}`, data);
    return response.data;
  },

  deleteFeed: async (id: number) => {
    const response = await api.delete(`${BASE_URL}/${id}`);
    return response.data;
  },

  toggleFeed: async (id: number) => {
    const response = await api.put<RssFeedConfigDto>(`${BASE_URL}/${id}/toggle`);
    return response.data;
  },

  resetFailures: async (id: number) => {
    const response = await api.post<RssFeedConfigDto>(`${BASE_URL}/${id}/reset-failures`);
    return response.data;
  },

  probeFeed: async (id: number) => {
    const response = await api.get<RssProbeDto>(`${BASE_URL}/${id}/probe`);
    return response.data;
  },

  // --- Processing ---
  processFeed: async (id: number) => {
    const response = await api.post(`${BASE_URL}/${id}/process`);
    return response.data;
  },

  processAllFeeds: async (maxArticles = 20, forceEnqueue = true) => {
    const response = await api.post(`${BASE_URL}/process-all`, null, {
      params: { maxArticles, forceEnqueue }
    });
    return response.data;
  },

  getProcessAllStatus: async () => {
    const response = await api.get<RssManualRunStatusDto>(`${BASE_URL}/process-all/status`);
    return response.data;
  },

  getProcessAllHistory: async (limit = 10) => {
    const response = await api.get<{ runs: RssManualRunStatusDto[] }>(`${BASE_URL}/process-all/history`, {
      params: { limit }
    });
    return response.data;
  },

  processQueue: async (maxItems = 5) => {
    const response = await api.post(`${BASE_URL}/process-queue`, null, {
      params: { maxItems }
    });
    return response.data;
  },

  getQueueStatus: async () => {
    const response = await api.get<RssQueueStatusDto>(`${BASE_URL}/queue-status`);
    return response.data;
  },

  getStatistics: async () => {
    const response = await api.get<RssStatsDto>(`${BASE_URL}/statistics`);
    return response.data;
  }
};
