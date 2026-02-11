import { useState, useEffect } from 'react';
import { RssProbeDto } from '@/shared/types/rssAdmin';
import { rssService } from '@/shared/api/rssService';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Loader2, AlertCircle, CheckCircle, ExternalLink, Clock } from 'lucide-react';
import { ScrollArea } from '@/components/ui/scroll-area';

interface RssProbeModalProps {
  feedId: number | null;
  onClose: () => void;
}

export const RssProbeModal = ({ feedId, onClose }: RssProbeModalProps) => {
  const [result, setResult] = useState<RssProbeDto | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (feedId) {
      setIsLoading(true);
      setError(null);
      setResult(null);
      rssService.probeFeed(feedId)
        .then(setResult)
        .catch((err) => {
            setError((err as any).response?.data?.message || (err as any).message || "Unknown error probing feed");
        })
        .finally(() => setIsLoading(false));
    }
  }, [feedId]);

  const isOpen = !!feedId;

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[600px] max-h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>Probe RSS Feed</DialogTitle>
          <DialogDescription>
            Validating feed connectivity and parsing structure.
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 overflow-hidden py-4">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center h-40 space-y-4">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <p className="text-sm text-muted-foreground">Contacting feed source...</p>
            </div>
          ) : error ? (
             <div className="flex flex-col items-center justify-center h-40 space-y-4 text-destructive">
              <AlertCircle className="h-8 w-8" />
              <p className="font-medium">Probe Failed</p>
              <p className="text-sm text-center px-4">{error}</p>
            </div>
          ) : result ? (
            <div className="space-y-4">
               <div className="flex items-center justify-between p-3 bg-muted rounded-md">
                  <div className="flex items-center gap-2">
                    {result.status === 'OK' ? <CheckCircle className="text-green-500 h-5 w-5" /> : <AlertCircle className="text-red-500 h-5 w-5" />}
                    <span className="font-semibold">{result.status}</span>
                  </div>
                  <div className="flex items-center gap-4 text-sm">
                      <span className="flex items-center gap-1"><Clock className="h-3 w-3" /> {result.durationMs}ms</span>
                      <Badge variant="outline">{result.itemCount} items</Badge>
                  </div>
               </div>

               {result.error && (
                   <div className="p-3 bg-red-50 border border-red-200 rounded-md text-red-800 text-sm">
                       {result.error}
                   </div>
               )}

               {result.sampleItems && result.sampleItems.length > 0 && (
                   <div className="space-y-2">
                       <h4 className="text-sm font-medium">Sample Items</h4>
                       <ScrollArea className="h-[200px] rounded-md border p-2">
                           <div className="space-y-3">
                               {result.sampleItems.map((item, idx) => (
                                   <div key={idx} className="text-sm border-b pb-2 last:border-0 last:pb-0">
                                       <a href={item.link} target="_blank" rel="noopener noreferrer" className="font-medium hover:underline flex items-center gap-1">
                                           {item.title} <ExternalLink className="h-3 w-3" />
                                       </a>
                                       <div className="text-xs text-muted-foreground mt-1">
                                           {item.pubDate ? new Date(item.pubDate).toLocaleString() : 'No Date'}
                                       </div>
                                   </div>
                               ))}
                           </div>
                       </ScrollArea>
                   </div>
               )}
            </div>
          ) : null}
        </div>

        <DialogFooter>
          <Button onClick={onClose}>Close</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
