import { Outlet, Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, Rss, Users, LogOut } from 'lucide-react';

import { Button } from '@/components/ui/button';

export const AdminLayout = () => {
  const location = useLocation();

  const navItems = [
    { href: '/admin', label: 'Dashboard', icon: LayoutDashboard },
    { href: '/admin/rss', label: 'RSS Feeds', icon: Rss },
    { href: '/admin/users', label: 'User Management', icon: Users },
  ];

  return (
    <div className="flex min-h-screen bg-background flex-col md:flex-row">
      {/* Mobile Header */}
      <div className="md:hidden flex h-14 items-center justify-between border-b px-4 bg-muted/30">
        <span className="font-bold">Admin Console</span>
        <Button variant="ghost" size="sm" asChild>
          <Link to="/feed" className="flex items-center gap-2 text-muted-foreground hover:text-primary">
            <LogOut className="h-4 w-4" /> Exit
          </Link>
        </Button>
      </div>

      {/* Sidebar */}
      <aside className="w-64 border-r bg-muted/30 hidden md:block h-screen sticky top-0">
        <div className="flex h-14 items-center justify-between border-b px-4">
          <span className="font-bold">Admin Console</span>
          <Button variant="ghost" size="icon" asChild title="Back to App">
            <Link to="/feed">
              <LogOut className="h-4 w-4 text-muted-foreground hover:text-primary" />
            </Link>
          </Button>
        </div>
        <div className="p-4 space-y-2">
           {navItems.map(item => (
             <Button
               key={item.href}
               variant={location.pathname === item.href ? "secondary" : "ghost"}
               className="w-full justify-start"
               asChild
             >
               <Link to={item.href}>
                 <item.icon className="mr-2 h-4 w-4" />
                 {item.label}
               </Link>
             </Button>
           ))}
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto">
         <div className="container py-6 pl-4 pr-4">
           <Outlet />
         </div>
      </main>
    </div>
  );
};
