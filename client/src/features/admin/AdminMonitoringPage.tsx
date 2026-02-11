import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  Play, Loader2, AlertCircle, CheckCircle, 
  Database, List, XCircle, RotateCcw, Activity as ActivityIcon
} from 'lucide-react';
import { MonitoringRun, ProcessAllResponse, QueueStatus } from '@/shared/types';
import api from '@/shared/api/client';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import { cn } from '@/shared/utils/cn';

export const AdminMonitoringPage = () => {
  const queryClient = useQueryClient();
  const [maxArticles, setMaxArticles] = useState(20);
  const [forceEnqueue, setForceEnqueue] = useState(false);
  const [activeRunId, setActiveRunId] = useState<string | null>(null);

  // Poll for active run status ONLY if we think a run is active or recently started
  const { data: runStatus, isError: isRunError } = useQuery<MonitoringRun>({
    queryKey: ['admin-monitor-status'],
    queryFn: async () => {
      const res = await api.get<MonitoringRun>('/api/admin/rss-feeds/process-all/status');
      return res.data;
    },
    refetchInterval: (query) => {
      const data = query.state.data as MonitoringRun | undefined;
      return data?.status === 'RUNNING' ? 2000 : 10000;
    },
  });

  // Derived active state
  useEffect(() => {
    if (runStatus?.status === 'RUNNING') {
      setActiveRunId(runStatus.runId);
    } else if (runStatus && activeRunId) {
      // Run just finished
      setActiveRunId(null);
      queryClient.invalidateQueries({ queryKey: ['admin-monitor-history'] });
      queryClient.invalidateQueries({ queryKey: ['admin-queue-status'] });
    }
  }, [runStatus, activeRunId, queryClient]);


  const { data: runHistory } = useQuery({
    queryKey: ['admin-monitor-history'],
    queryFn: async () => {
      const res = await api.get<{ runs: MonitoringRun[] }>('/api/admin/rss-feeds/process-all/history?limit=10');
      return res.data;
    },
  });

  const { data: queueStatus } = useQuery({
    queryKey: ['admin-queue-status'],
    queryFn: async () => {
      const res = await api.get<QueueStatus>('/api/admin/rss-feeds/queue-status');
      return res.data;
    },
    refetchInterval: 5000,
  });

  const startRunMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post<ProcessAllResponse>(
        `/api/admin/rss-feeds/process-all?maxArticles=${maxArticles}&forceEnqueue=${forceEnqueue}`
      );
      return res.data;
    },
    onSuccess: (data) => {
      setActiveRunId(data.run.runId);
      queryClient.invalidateQueries({ queryKey: ['admin-monitor-status'] });
    },
    onError: (err: any) => {
      alert(err.response?.data?.message || 'Failed to start run');
    },
  });

  const drainQueueMutation = useMutation({
    mutationFn: async () => {
        await api.post('/api/admin/rss-feeds/process-queue?maxItems=10');
    },
    onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['admin-queue-status'] });
    }
  });

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'RUNNING': return 'bg-blue-500 text-blue-50';
      case 'COMPLETED': return 'bg-green-500 text-green-50';
      case 'FAILED': return 'bg-red-500 text-red-50';
      default: return 'bg-gray-500 text-gray-50';
    }
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight">System Monitoring</h1>
        <p className="text-muted-foreground">Real-time status of RSS ingestion, processing queue, and job history.</p>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {/* ═══ CONTROL PANEL ═══ */}
        <Card className="md:col-span-1 border-warm-200 shadow-sm">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
               <RotateCcw className="h-5 w-5 text-primary" /> Manual Execution
            </CardTitle>
            <CardDescription>Trigger a new RSS ingestion and processing run.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="space-y-4">
               <div className="grid gap-2">
                 <Label>Max Articles to Process (Global)</Label>
                 <Input 
                   type="number" 
                   value={maxArticles} 
                   onChange={(e) => setMaxArticles(Number(e.target.value))}
                   min={1} 
                   max={200}
                 />
                 <p className="text-[10px] text-muted-foreground">Limits the total number of articles fetched across all feeds.</p>
               </div>
               
               <div className="flex items-center space-x-2">
                 <Switch id="force-enqueue" checked={forceEnqueue} onCheckedChange={setForceEnqueue} />
                 <Label htmlFor="force-enqueue">Force Re-queue</Label>
               </div>
            </div>

            <Button 
              onClick={() => startRunMutation.mutate()} 
              disabled={startRunMutation.isPending || runStatus?.status === 'RUNNING'}
              className="w-full"
              size="lg"
            >
              {startRunMutation.isPending ? (
                 <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Starting...</>
              ) : runStatus?.status === 'RUNNING' ? (
                 <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Job in Progress</>
              ) : (
                 <><Play className="mr-2 h-4 w-4" /> Start Monitoring Run</>
              )}
            </Button>

            {isRunError && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>Error</AlertTitle>
                <AlertDescription>Failed to check run status. Backend might be down.</AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>

        {/* ═══ LIVE STATUS ═══ */}
        <Card className="md:col-span-1 border-warm-200 shadow-sm">
            <CardHeader>
                <CardTitle className="flex items-center gap-2">
                    <ActivityIcon className="h-5 w-5 text-primary" /> Live Status
                </CardTitle>
                <CardDescription>Current job progress and queue metrics.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                {runStatus ? (
                    <div className="space-y-4">
                        <div className="flex items-center justify-between pb-2 border-b">
                            <span className="text-sm font-medium text-muted-foreground">Status</span>
                            <Badge className={getStatusColor(runStatus.status)}>{runStatus.status}</Badge>
                        </div>
                        <div className="space-y-2">
                           <div className="flex justify-between text-xs">
                              <span>Progress ({Math.round(runStatus.progressPercent)}%)</span>
                              <span className="text-muted-foreground uppercase">{runStatus.stage}</span>
                           </div>
                           <Progress value={runStatus.progressPercent} className="h-2" />
                        </div>
                        <div className="grid grid-cols-2 gap-4 text-sm mt-4">
                            <div className="bg-muted/30 p-2 rounded">
                                <span className="block text-muted-foreground text-xs">Processed</span>
                                <span className="text-xl font-bold">{runStatus.processed}</span>
                            </div>
                            <div className="bg-muted/30 p-2 rounded">
                                <span className="block text-muted-foreground text-xs">Queue Size</span>
                                <span className="text-xl font-bold">{runStatus.queueSize}</span>
                            </div>
                            <div className="bg-muted/30 p-2 rounded">
                                <span className="block text-muted-foreground text-xs">Dropped</span>
                                <span className="text-xl font-bold text-orange-600">{runStatus.dropped}</span>
                            </div>
                            <div className="bg-muted/30 p-2 rounded">
                                <span className="block text-muted-foreground text-xs">Errors</span>
                                <span className="text-xl font-bold text-red-600">{runStatus.requeued}</span>
                            </div>
                        </div>
                        {runStatus.error && (
                            <Alert variant="destructive" className="mt-2">
                                <AlertTitle>Run Error</AlertTitle>
                                <AlertDescription>{runStatus.error}</AlertDescription>
                            </Alert>
                        )}
                    </div>
                ) : (
                    <div className="h-[200px] flex items-center justify-center text-muted-foreground text-sm">
                        Waiting for status data...
                    </div>
                )}
            </CardContent>
        </Card>
      </div>

      {/* ═══ QUEUE & HISTORY ═══ */}
      <div className="grid gap-6 md:grid-cols-3">
          {/* QUEUE VISUALIZATION */}
          <Card className="md:col-span-1">
              <CardHeader>
                  <CardTitle className="text-base flex items-center gap-2">
                      <Database className="h-4 w-4" /> Queue Metrics
                  </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                  <div className="text-center py-4 bg-muted/20 rounded-lg">
                      <div className="text-4xl font-extrabold text-primary">{queueStatus?.queueSize || 0}</div>
                      <div className="text-xs text-muted-foreground mt-1">Pending Items</div>
                  </div>
                  <div className="space-y-2 text-sm">
                      <div className="flex justify-between">
                          <span className="text-muted-foreground">Total Enqueued</span>
                          <span className="font-mono">{queueStatus?.totalEnqueued || 0}</span>
                      </div>
                      <div className="flex justify-between">
                          <span className="text-muted-foreground">Total Processed</span>
                          <span className="font-mono">{queueStatus?.totalProcessed || 0}</span>
                      </div>
                      <div className="flex justify-between">
                           <span className="text-muted-foreground">Processing Active</span>
                           <Badge variant={queueStatus?.processing ? "default" : "secondary"}>
                               {queueStatus?.processing ? "YES" : "NO"}
                           </Badge>
                      </div>
                  </div>
                  <Button variant="outline" size="sm" className="w-full" onClick={() => drainQueueMutation.mutate()} disabled={drainQueueMutation.isPending}>
                       Force Drain Queue (10 items)
                  </Button>
              </CardContent>
          </Card>

          {/* HISTORY TABLE */}
          <Card className="md:col-span-2">
              <CardHeader>
                  <CardTitle className="text-base flex items-center gap-2">
                      <List className="h-4 w-4" /> Recent Runs
                  </CardTitle>
              </CardHeader>
              <CardContent>
                  <ScrollArea className="h-[300px]">
                      <div className="space-y-1">
                          {runHistory?.runs.map((run) => (
                              <div key={run.runId} className="flex items-center justify-between p-3 rounded-lg border hover:bg-muted/40 transition-colors">
                                  <div className="flex items-center gap-3">
                                      {run.status === 'COMPLETED' ? <CheckCircle className="h-4 w-4 text-green-500" /> : 
                                       run.status === 'FAILED' ? <XCircle className="h-4 w-4 text-red-500" /> :
                                       <Loader2 className="h-4 w-4 animate-spin text-blue-500" />}
                                      <div>
                                          <p className="text-sm font-medium">{new Date(run.startedAt).toLocaleString()}</p>
                                          <p className="text-xs text-muted-foreground">
                                              {run.processed} processed • {run.dropped} dropped • {run.durationSeconds?.toFixed(1) || '-'}s duration
                                          </p>
                                      </div>
                                  </div>
                                  <Badge variant="outline" className={cn("text-xs", getStatusColor(run.status))}>
                                      {run.status}
                                  </Badge>
                              </div>
                          ))}
                          {(!runHistory?.runs || runHistory.runs.length === 0) && (
                              <div className="text-center py-8 text-muted-foreground text-sm">
                                  No run history available.
                              </div>
                          )}
                      </div>
                  </ScrollArea>
              </CardContent>
          </Card>
      </div>
    </div>
  );
};


