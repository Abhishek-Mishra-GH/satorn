import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { rssService } from '@/shared/api/rssService';
import { RssManualRunStatusDto } from '@/shared/types/rssAdmin';
import { Play, Loader2, Clock } from 'lucide-react';
import { Progress } from '@/components/ui/progress';

interface RssRunControlProps {
  onRunStatusChange: (status: RssManualRunStatusDto | undefined) => void;
  currentRun?: RssManualRunStatusDto;
}

export const RssRunControl = ({ onRunStatusChange, currentRun }: RssRunControlProps) => {
  const [maxArticles, setMaxArticles] = useState(20);
  const [forceEnqueue, setForceEnqueue] = useState(true);
  const [isStarting, setIsStarting] = useState(false);
  const [pollInterval, setPollInterval] = useState<NodeJS.Timeout | null>(null);

  const isRunning = currentRun?.status === 'RUNNING';

  useEffect(() => {
    // Initial status check
    rssService.getProcessAllStatus()
      .then(onRunStatusChange)
      .catch(() => {});
      
    return () => {
      if (pollInterval) clearInterval(pollInterval);
    };
  }, []);

  useEffect(() => {
    // Polling logic
    if (isRunning) {
      const interval = setInterval(async () => {
        try {
            const status = await rssService.getProcessAllStatus();
            onRunStatusChange(status);
            if (status.status !== 'RUNNING') {
                clearInterval(interval);
            }
        } catch (e) {
            console.error("Polling error", e);
        }
      }, 2000);
      setPollInterval(interval);
      return () => clearInterval(interval);
    }
  }, [isRunning, onRunStatusChange]);

  const handleStartRun = async () => {
    setIsStarting(true);
    try {
      await rssService.processAllFeeds(maxArticles, forceEnqueue);
      const status = await rssService.getProcessAllStatus();
      onRunStatusChange(status);
    } catch (error) {
      console.error("Failed to start run", error);
      alert("Failed to start run: " + (error as any).response?.data?.message || (error as any).message);
    } finally {
      setIsStarting(false);
    }
  };

  return (
    <Card className="h-full">
      <CardHeader className="pb-3">
        <CardTitle>Manual Processing</CardTitle>
        <CardDescription>Trigger a scan of all enabled feeds</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div className="flex flex-col gap-4 p-4 border rounded-md bg-muted/20">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="maxArticles">Max Articles</Label>
                <Input 
                  id="maxArticles" 
                  type="number" 
                  value={maxArticles} 
                  onChange={(e) => setMaxArticles(Number(e.target.value))}
                  disabled={isRunning || isStarting}
                  min={1}
                  max={100}
                />
              </div>
              <div className="flex items-center space-x-2 pt-8">
                <Checkbox 
                  id="force" 
                  checked={forceEnqueue} 
                  onCheckedChange={(c) => setForceEnqueue(!!c)} 
                  disabled={isRunning || isStarting}
                />
                <Label htmlFor="force" className="cursor-pointer">Force Scan</Label>
              </div>
            </div>
            
            <Button 
                onClick={handleStartRun} 
                disabled={isRunning || isStarting} 
                className="w-full"
            >
              {isStarting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : 
               isRunning ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : 
               <Play className="mr-2 h-4 w-4" />}
              {isRunning ? 'Run in Progress...' : 'Start Manual Run'}
            </Button>
          </div>

          {currentRun && currentRun.status !== 'IDLE' && (
             <div className="space-y-3 pt-2">
                <div className="flex justify-between items-center text-sm">
                    <span className={`font-medium ${currentRun.status === 'RUNNING' ? 'text-blue-500' : currentRun.status === 'COMPLETED' ? 'text-green-500' : 'text-red-500'}`}>
                        {currentRun.status} - {currentRun.stage}
                    </span>
                    <span className="text-muted-foreground text-xs flex items-center">
                        <Clock className="h-3 w-3 mr-1" />
                        {currentRun.updatedAt ? new Date(currentRun.updatedAt).toLocaleTimeString() : ''}
                    </span>
                </div>
                <Progress value={currentRun.progressPercent || 0} className="h-2" />
                <div className="text-xs text-muted-foreground grid grid-cols-3 gap-2 text-center">
                    <div>
                        <span className="font-bold block text-foreground">{currentRun.processed || 0}</span>
                        Processed
                    </div>
                    <div>
                        <span className="font-bold block text-foreground">{currentRun.queueSize || 0}</span>
                        Queued
                    </div>
                     <div>
                        <span className="font-bold block text-foreground">{currentRun.dropped || 0}</span>
                        Dropped
                    </div>
                </div>
             </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};
