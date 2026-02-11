import { Outlet } from 'react-router-dom';

export const AuthLayout = () => {
  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-background px-4">
      <div className="w-full max-w-md space-y-8">
        <div className="text-center">
          <h1 className="text-4xl font-extrabold tracking-tight text-primary">SATORN</h1>
          <p className="mt-2 text-muted-foreground">AI-Powered News Verification</p>
        </div>
        <Outlet />
      </div>
    </div>
  );
};
