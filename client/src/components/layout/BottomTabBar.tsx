import { Link, useLocation } from 'react-router-dom';
import { Home, Bookmark, ShieldAlert, LayoutDashboard, School } from 'lucide-react';
import { useAuthStore } from '@/shared/store/authStore';
import { cn } from '@/shared/utils/cn';

export const BottomTabBar = () => {
  const { pathname } = useLocation();
  const { isAuthenticated, user } = useAuthStore();
  const isAdmin = user?.roles?.includes('ROLE_ADMIN');

  const tabs = [
    {
      label: 'Learning',
      icon: School,
      path: '/learning',
      show: true,
    },
    {
      label: 'Feed',
      icon: Home,
      path: '/feed',
      show: true,
    },
    {
      label: 'Debunk',
      icon: ShieldAlert,
      path: '/chat',
      show: true,
    },
    {
      label: 'Saved',
      icon: Bookmark,
      path: '/saved',
      show: isAuthenticated,
    },
    {
      label: 'Admin',
      icon: LayoutDashboard,
      path: '/admin',
      show: isAuthenticated && isAdmin,
    },
  ];

  const visibleTabs = tabs.filter((tab) => tab.show);

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 block md:hidden">
      <div className="flex items-center justify-around bg-white/90 backdrop-blur-lg border-t border-warm-200 px-2 py-3 pb-safe shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
        {visibleTabs.map((tab) => {
          const isActive = pathname === tab.path || (tab.path !== '/' && pathname.startsWith(tab.path));
          const Icon = tab.icon;

          return (
            <Link
              key={tab.path}
              to={tab.path}
              className={cn(
                "flex flex-col items-center justify-center w-full gap-1 transition-colors duration-200",
                isActive ? "text-primary" : "text-muted-foreground hover:text-foreground"
              )}
            >
              <div
                className={cn(
                  "p-1.5 rounded-xl transition-all duration-200",
                  isActive ? "bg-primary/10" : "bg-transparent"
                )}
              >
                <Icon className={cn("h-5 w-5", isActive && "fill-current")} />
              </div>
              <span className="text-[10px] font-medium">{tab.label}</span>
            </Link>
          );
        })}
      </div>
    </div>
  );
};
