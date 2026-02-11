import axios from 'axios';
import { useAuthStore } from '@/shared/store/authStore';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const { refreshToken, logout, isAuthenticated } = useAuthStore.getState();

      if (!refreshToken) {
        // Guest user — don't logout or redirect, just reject silently
        if (isAuthenticated) logout();
        return Promise.reject(error);
      }

      try {
        const response = await axios.post<{ accessToken: string; refreshToken: string }>(
          `${api.defaults.baseURL}/api/auth/refresh`,
          { refreshToken }
        );

        const { accessToken, refreshToken: newRefreshToken } = response.data;
        
        // Update store with new tokens (keeping existing user data if not returned)
        // Note: The prompt says refresh returns same as login, so we might get full user object too.
        // We'll update what we have.
        useAuthStore.getState().setTokens(accessToken, newRefreshToken);

        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        logout();
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default api;
