import { Link, useNavigate } from 'react-router-dom';
import { LogOut, LayoutDashboard, Newspaper, ShieldAlert, User, Bookmark, School } from 'lucide-react';
import { useAuthStore } from '@/shared/store/authStore';
import { Button } from '@/components/ui/button';

export const Header = () => {
  const { isAuthenticated, user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isAdmin = user?.roles?.includes('ROLE_ADMIN');

  return (
    <header className="sticky top-0 z-50 w-full bg-white/95 backdrop-blur supports-[backdrop-filter]:bg-white/80 border-b border-warm-100">
      <div className="container flex h-16 justify-between sm:items-center px-6">
        {/* Left: Logo */}
        <Link to="/" className="flex items-center gap-2 flex-shrink-0">
          <span className="text-2xl font-extrabold tracking-tight text-primary" style={{ fontFamily: 'Outfit, sans-serif' }}>
            SATORN
          </span>
        </Link>

        {/* Center: Main nav links */}
        <nav className="hidden md:flex flex-1 items-center justify-center gap-1">
          <Button variant="ghost" size="sm" asChild className="rounded-full">
            <Link to="/learning">
               <School className="mr-2 h-4 w-4" /> Learning
            </Link>
          </Button>
          <Button variant="ghost" size="sm" asChild className="rounded-full">
            <Link to="/feed">
              <Newspaper className="mr-2 h-4 w-4" /> Feed
            </Link>
          </Button>
          <Button variant="ghost" size="sm" asChild className="rounded-full">
            <Link to="/chat">
              <ShieldAlert className="mr-2 h-4 w-4" /> Debunk
            </Link>
          </Button>
          {isAuthenticated && (
            <>
              <Button variant="ghost" size="sm" asChild className="rounded-full">
                <Link to="/saved">
                  <Bookmark className="mr-2 h-4 w-4" /> Saved
                </Link>
              </Button>
              {isAdmin && (
                <Button variant="ghost" size="sm" asChild className="rounded-full">
                  <Link to="/admin">
                    <LayoutDashboard className="mr-2 h-4 w-4" /> Admin
                  </Link>
                </Button>
              )}
            </>
          )}
        </nav>

        {/* Right: User actions */}
        <div className="flex items-center gap-2 flex-shrink-0">
          {isAuthenticated ? (
            <Button variant="outline" size="sm" onClick={handleLogout} className="rounded-full border-warm-200">
              <LogOut className="mr-2 h-4 w-4" /> Logout
            </Button>
          ) : (
            <Link to="/login" className="flex items-center justify-center h-9 w-9 rounded-full hover:bg-warm-100 transition-colors">
              <User className="h-5 w-5 text-primary" />
            </Link>
          )}
        </div>
      </div>
    </header>
  );
};
