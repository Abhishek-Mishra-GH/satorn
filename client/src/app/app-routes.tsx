import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { MainLayout } from '@/components/layout/MainLayout';
import { AuthLayout } from '@/features/auth/AuthLayout';
import { LoginPage } from '@/features/auth/LoginPage';
import { RegisterPage } from '@/features/auth/RegisterPage';
import { LandingPage } from '@/features/LandingPage';
import { FeedPage } from '@/features/feed/FeedPage';
import { SavedArticlesPage } from '@/features/feed/SavedArticlesPage';
import { ArticlePage } from '@/features/article/ArticlePage';
import { ChatPage } from '@/features/chat/ChatPage';
import { AdminLayout } from '@/features/admin/AdminLayout';
import { AdminDashboardPage } from '@/features/admin/AdminDashboardPage';
import { RssFeedPage } from '@/features/admin/RssFeedPage';
import { UserManagementPage } from '@/features/admin/UserManagementPage';
import { AdminMonitoringPage } from '@/features/admin/AdminMonitoringPage';
import { ProtectedRoute } from '@/shared/components/ProtectedRoute';
import { LearningDashboard } from '@/features/learning/pages/LearningDashboard';
import { LearningProfilePage } from '@/features/learning/pages/LearningProfilePage';
import { LearningFeedPage } from '@/features/learning/pages/LearningFeedPage';
import { QuizPage } from '@/features/learning/pages/QuizPage';
import { SkillsPage } from '@/features/learning/pages/SkillsPage';

const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <LandingPage /> },
      // Public routes — no auth required
      { path: 'feed', element: <FeedPage /> },
      { path: 'articles/:id', element: <ArticlePage /> },
      { path: 'chat', element: <ChatPage /> },
      // Auth-required routes
      {
        path: 'saved',
        element: <ProtectedRoute allowedRoles={['ROLE_USER', 'ROLE_ADMIN']} />,
        children: [{ index: true, element: <SavedArticlesPage /> }]
      },
      {
        path: 'learning',
        element: <ProtectedRoute allowedRoles={['ROLE_USER', 'ROLE_ADMIN']} />,
        children: [
            { index: true, element: <LearningDashboard /> },
            { path: 'profile', element: <LearningProfilePage /> },
            { path: 'feed', element: <LearningFeedPage /> },
            { path: 'quiz', element: <QuizPage /> },
            { path: 'skills', element: <SkillsPage /> },
        ]
      },
    ],
  },
  {
    element: <AuthLayout />,
    children: [
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
    ],
  },
  {
    path: 'admin',
    element: <ProtectedRoute allowedRoles={['ROLE_ADMIN']} />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { index: true, element: <AdminDashboardPage /> },
          { path: 'rss', element: <RssFeedPage /> },
          { path: 'users', element: <UserManagementPage /> },
          { path: 'monitoring', element: <AdminMonitoringPage /> },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <div className="flex h-screen items-center justify-center">404 - Not Found</div>
  }
]);

export const AppRouter = () => {
    return <RouterProvider router={router} />;
};
