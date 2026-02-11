import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { RssStatsDto } from '@/shared/types/rssAdmin';
import { Activity, AlertTriangle, CheckCircle, FileText, Rss } from 'lucide-react';

interface RssStatsPanelProps {
  stats?: RssStatsDto;
  isLoading: boolean;
}

export const RssStatsPanel = ({ stats, isLoading }: RssStatsPanelProps) => {
  if (isLoading) {
    return <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
       {[...Array(5)].map((_, i) => (
         <Card key={i} className="animate-pulse">
           <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
             <div className="h-4 w-20 bg-muted rounded"></div>
           </CardHeader>
           <CardContent>
             <div className="h-8 w-12 bg-muted rounded"></div>
           </CardContent>
         </Card>
       ))}
    </div>;
  }

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Total Feeds</CardTitle>
          <Rss className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{stats?.totalFeeds || 0}</div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Enabled</CardTitle>
          <CheckCircle className="h-4 w-4 text-green-500" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{stats?.enabledFeeds || 0}</div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Disabled</CardTitle>
          <Activity className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{stats?.disabledFeeds || 0}</div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Articles</CardTitle>
          <FileText className="h-4 w-4 text-blue-500" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{stats?.totalArticlesProcessed || 0}</div>
          <p className="text-xs text-muted-foreground">Processed total</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Errors</CardTitle>
          <AlertTriangle className="h-4 w-4 text-red-500" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{stats?.feedsWithErrors || 0}</div>
          <p className="text-xs text-muted-foreground">Feeds failing</p>
        </CardContent>
      </Card>
    </div>
  );
};
