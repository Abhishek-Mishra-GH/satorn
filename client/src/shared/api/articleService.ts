import api from './client';
import { Article } from '../types';

export interface ArticleQueryParams {
  page?: number;
  size?: number;
  category?: string;
  status?: string;
}

export interface SearchQueryParams extends ArticleQueryParams {
  query: string;
}

export interface ArticlesResponse {
  total: number;
  page?: number;
  size?: number;
  articles: Article[];
  category?: string;
}

export const articleService = {
  getSynthesizedArticles: async (params?: ArticleQueryParams) => {
    const response = await api.get<ArticlesResponse>('/api/synthesized-articles', { params });
    return response.data;
  },

  getTrendingArticles: async (params?: ArticleQueryParams) => {
    const response = await api.get<ArticlesResponse>('/api/synthesized-articles/trending', { params });
    return response.data;
  },

  getTopCredibleArticles: async (params?: ArticleQueryParams) => {
    const response = await api.get<ArticlesResponse>('/api/synthesized-articles/top-credible', { params });
    return response.data;
  },

  getArticlesByCategory: async (category: string, params?: ArticleQueryParams) => {
    const response = await api.get<ArticlesResponse>(`/api/synthesized-articles/category/${category}`, { params });
    return response.data;
  },

  searchArticles: async (params: SearchQueryParams) => {
    const response = await api.get<ArticlesResponse>('/api/synthesized-articles/search', { params });
    return response.data;
  }
};
