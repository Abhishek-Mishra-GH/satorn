import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { RssQueueStatusDto } from '@/shared/types/rssAdmin';
import { RefreshCw, Database, Layers, Zap } from 'lucide-react';
import { Progress } from '@/components/ui/progress';

interface RssQueueStatusProps {
  status?: RssQueueStatusDto;
  isLoading: boolean;
  onRefresh: () => void;
}

export const RssQueueStatus = ({ status, isLoading, onRefresh }: RssQueueStatusProps) => {
  const queuePercent = status ? Math.min((status.queueSize / status.queueCapacity) * 100, 100) : 0;

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Queue Status</CardTitle>
            <CardDescription>Real-time processing metrics</CardDescription>
          </div>
          <Button variant="ghost" size="icon" onClick={onRefresh} disabled={isLoading}>
            <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">Queue Usage</span>
            <span className="font-medium">{status?.queueSize || 0} / {status?.queueCapacity || 0}</span>
          </div>
          <Progress value={queuePercent} className="h-2" />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="flex flex-col space-y-1.5 p-3 border rounded-md bg-muted/20">
            <span className="text-xs text-muted-foreground flex items-center gap-1">
              <Layers className="h-3 w-3" /> Enqueued
            </span>
            <span className="text-xl font-bold">{status?.totalEnqueued || 0}</span>
          </div>
          <div className="flex flex-col space-y-1.5 p-3 border rounded-md bg-muted/20">
            <span className="text-xs text-muted-foreground flex items-center gap-1">
              <Database className="h-3 w-3" /> Processed
            </span>
            <span className="text-xl font-bold">{status?.totalProcessed || 0}</span>
          </div>
          <div className="flex flex-col space-y-1.5 p-3 border rounded-md bg-muted/20">
             <span className="text-xs text-muted-foreground flex items-center gap-1">
              <Zap className="h-3 w-3" /> Active
            </span>
             <span className="text-sm font-medium flex items-center gap-2">
               {status?.processing ? (
                 <span className="flex h-2 w-2 rounded-full bg-green-500 animate-pulse" />
               ) : (
                 <span className="flex h-2 w-2 rounded-full bg-gray-300" />
               )}
               {status?.processing ? 'Processing' : 'Idle'}
             </span>
          </div>
           <div className="flex flex-col space-y-1.5 p-3 border rounded-md bg-muted/20">
            <span className="text-xs text-muted-foreground flex items-center gap-1">
              Rate Limits
            </span>
            <span className="text-xl font-bold">{status?.totalRateLimited || 0}</span>
          </div>
        </div>
        
        {status?.rateLimiter && Object.keys(status.rateLimiter).length > 0 && (
          <div className="space-y-2">
            <h4 className="text-xs font-semibold uppercase text-muted-foreground">Rate Limiters</h4>
            <div className="grid gap-2 text-sm">
                {Object.entries(status.rateLimiter).map(([key, val]) => (
                    <div key={key} className="flex justify-between border-b pb-1 last:border-0">
                        <span className="capitalize">{key}</span>
                        <span className="font-mono text-xs">{val.availableTokens?.toFixed(1) ?? '?'} / {val.maxTokens ?? '?'}</span>
                    </div>
                ))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
