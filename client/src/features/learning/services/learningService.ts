import api from '@/shared/api/client';
import {
  LearnerProfile,
  UpdateProfileRequest,
  RecommendationsResponse,
  QuizGenerateRequest,
  QuizSession,
  QuizSubmitRequest,
  QuizResult,
  SkillsResponse,
  TutorRequest,
  TutorResponse,
} from '../types';

const BASE_URL = '/api/learning';

export const educationService = {
  getProfile: async (): Promise<LearnerProfile> => {
    const response = await api.get<LearnerProfile>(`${BASE_URL}/profile`);
    return response.data;
  },

  updateProfile: async (data: UpdateProfileRequest): Promise<LearnerProfile> => {
    const response = await api.put<LearnerProfile>(`${BASE_URL}/profile`, data);
    return response.data;
  },

  getRecommendations: async (page = 0, size = 10): Promise<RecommendationsResponse> => {
    const response = await api.get<RecommendationsResponse>(`${BASE_URL}/recommendations`, {
      params: { page, size },
    });
    return response.data;
  },

  generateQuiz: async (data: QuizGenerateRequest): Promise<QuizSession> => {
    const response = await api.post<QuizSession>(`${BASE_URL}/quiz/generate`, data);
    return response.data;
  },

  submitQuiz: async (data: QuizSubmitRequest): Promise<QuizResult> => {
    const response = await api.post<QuizResult>(`${BASE_URL}/quiz/submit`, data);
    return response.data;
  },

  getSkills: async (): Promise<SkillsResponse> => {
    const response = await api.get<SkillsResponse>(`${BASE_URL}/skills`);
    return response.data;
  },

  askTutor: async (data: TutorRequest): Promise<TutorResponse> => {
    const response = await api.post<TutorResponse>(`${BASE_URL}/tutor`, data);
    return response.data;
  },
};
