import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { User, AuthResponse } from '@/shared/types';
import api from '@/shared/api/client';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  setAuth: (data: AuthResponse) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      setAuth: (data) =>
        set({
          user: {
            id: data.id,
            username: data.username,
            email: data.email,
            roles: data.roles,
          },
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          isAuthenticated: true,
        }),
      setTokens: (accessToken, refreshToken) =>
        set((state) => ({ ...state, accessToken, refreshToken })),
      logout: () => {
         // Attempt server logout, but don't block client logout
         const { refreshToken } = useAuthStore.getState();
         if(refreshToken) {
             api.post('/api/auth/logout', { refreshToken }).catch(console.error);
         }
         
         set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        })
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ // Only persist tokens and user info
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
