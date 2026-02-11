import { Outlet, useLocation } from 'react-router-dom';
import { Header } from './Header';
import { Footer } from './Footer';
import { BottomTabBar } from './BottomTabBar';

export const MainLayout = () => {
  const { pathname } = useLocation();
  const hideFooter = pathname === '/chat';

  return (
    <div className="relative flex min-h-screen flex-col pb-16 md:pb-0">
      <Header />
      <div className="flex-1">
        <Outlet />
      </div>
      {!hideFooter && <Footer />}
      <BottomTabBar />
    </div>
  );
};
