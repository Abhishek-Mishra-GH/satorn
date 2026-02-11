import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/shared/store/authStore';

interface ProtectedRouteProps {
  allowedRoles?: string[];
}

export const ProtectedRoute = ({ allowedRoles }: ProtectedRouteProps) => {
  const { isAuthenticated, user } = useAuthStore();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && user && !allowedRoles.some(role => user.roles.includes(role))) {
    // User authorized but not with correct role
    return <Navigate to="/" replace />; // Or safe unatuhorized page
  }

  return <Outlet />;
};
