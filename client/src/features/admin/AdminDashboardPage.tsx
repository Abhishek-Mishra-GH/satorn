import { useQuery } from '@tanstack/react-query';
import { Activity, Users, Database } from 'lucide-react';
import api from '@/shared/api/client';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export const AdminDashboardPage = () => {
  // Fetch stats (mocking combined stats or individual calls)
  const { data: feedStats } = useQuery({
      queryKey: ['admin-stats-feeds'],
      queryFn: async () => {
          const res = await api.get('/api/admin/rss-feeds/statistics');
          return res.data;
      }
  });

   const { data: queueStatus } = useQuery({
      queryKey: ['admin-queue-status'],
      queryFn: async () => {
          const res = await api.get('/api/admin/rss-feeds/queue-status');
          return res.data;
      },
      refetchInterval: 5000 // Poll every 5s
  });

  return (
    <div className="space-y-6">
       <div>
         <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
         <p className="text-muted-foreground">System overview and statistics.</p>
       </div>

       <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Feeds</CardTitle>
              <RssIcon className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{feedStats?.totalFeeds || '-'}</div>
              <p className="text-xs text-muted-foreground">
                {feedStats?.activeFeeds || 0} active
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Queue Size</CardTitle>
              <Activity className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{queueStatus?.queueSize || 0}</div>
              <p className="text-xs text-muted-foreground">
                Items pending processing
              </p>
            </CardContent>
          </Card>
           <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Articles Synthesized</CardTitle>
              <Database className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{feedStats?.articlesProcessed || '-'}</div>
              <p className="text-xs text-muted-foreground">
                +12% from last month (mock)
              </p>
            </CardContent>
          </Card>
           <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Users</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">-</div>
              <p className="text-xs text-muted-foreground">
                Registered users
              </p>
            </CardContent>
          </Card>
       </div>
    </div>
  );
};

function RssIcon(props: any) {
  return (
    <svg
      {...props}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M4 11a9 9 0 0 1 9 9" />
      <path d="M4 4a16 16 0 0 1 16 16" />
      <circle cx="5" cy="19" r="1" />
    </svg>
  )
}
