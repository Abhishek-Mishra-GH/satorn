import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { rssService } from '@/shared/api/rssService';
import { RssFeedConfigDto, RssManualRunStatusDto } from '@/shared/types/rssAdmin';
import { RssStatsPanel } from './rss/RssStatsPanel';
import { RssQueueStatus } from './rss/RssQueueStatus';
import { RssRunControl } from './rss/RssRunControl';
import { RssFeedTable } from './rss/RssFeedTable';
import { RssFeedModal } from './rss/RssFeedModal';
import { RssProbeModal } from './rss/RssProbeModal';
import { useToast } from '@/hooks/use-toast';

export const RssFeedPage = () => {
    const queryClient = useQueryClient();
    const { toast } = useToast();
    
    // State
    const [isAddOpen, setIsAddOpen] = useState(false);
    const [editingFeed, setEditingFeed] = useState<RssFeedConfigDto | undefined>(undefined);
    const [probingFeedId, setProbingFeedId] = useState<number | null>(null);
    const [currentRun, setCurrentRun] = useState<RssManualRunStatusDto | undefined>(undefined);

    // Queries
    const { data: feeds = [], isLoading: feedsLoading } = useQuery({
        queryKey: ['rss-feeds'],
        queryFn: rssService.getFeeds
    });

    const { data: stats, isLoading: statsLoading } = useQuery({
        queryKey: ['rss-stats'],
        queryFn: rssService.getStatistics
    });

    const { data: queueStatus, isLoading: queueLoading, refetch: refetchQueue } = useQuery({
        queryKey: ['rss-queue'],
        queryFn: rssService.getQueueStatus,
        refetchInterval: 5000 // Poll queue status
    });

    // Mutations
    const deleteMutation = useMutation({
        mutationFn: rssService.deleteFeed,
        onSuccess: () => {
             queryClient.invalidateQueries({ queryKey: ['rss-feeds'] });
             queryClient.invalidateQueries({ queryKey: ['rss-stats'] });
             toast({ title: "Feed deleted" });
        },
        onError: (err: any) => {
            toast({ title: "Error deleting feed", description: err.message, variant: "destructive" });
        }
    });

    const toggleMutation = useMutation({
        mutationFn: rssService.toggleFeed,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['rss-feeds'] });
            queryClient.invalidateQueries({ queryKey: ['rss-stats'] });
            toast({ title: "Feed status updated" });
        }
    });
    
    const resetFailuresMutation = useMutation({
        mutationFn: rssService.resetFailures,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['rss-feeds'] });
            toast({ title: "Failures reset" });
        }
    });

    const processMutation = useMutation({
        mutationFn: rssService.processFeed,
        onSuccess: (data) => {
             toast({ title: "Feed processing queued", description: data.message });
             refetchQueue();
        },
        onError: (err: any) => {
             toast({ title: "Error processing feed", description: err.message, variant: "destructive" });
        }
    });

    // Handlers
    const handleEdit = (feed: RssFeedConfigDto) => {
        setEditingFeed(feed);
        setIsAddOpen(true);
    };

    const handleCloseModal = () => {
        setIsAddOpen(false);
        setEditingFeed(undefined);
    };

    const handleSuccessModal = () => {
        queryClient.invalidateQueries({ queryKey: ['rss-feeds'] });
        queryClient.invalidateQueries({ queryKey: ['rss-stats'] });
        toast({ title: editingFeed ? "Feed updated" : "Feed created" });
    };

    return (
        <div className="space-y-6 p-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">RSS Management</h1>
                    <p className="text-muted-foreground">Monitor and configure RSS feed ingestion.</p>
                </div>
                <Button onClick={() => setIsAddOpen(true)}>
                    <Plus className="mr-2 h-4 w-4" /> Add Feed
                </Button>
            </div>

            <RssStatsPanel stats={stats} isLoading={statsLoading} />

            <div className="grid gap-6 md:grid-cols-2">
                <RssQueueStatus 
                    status={queueStatus} 
                    isLoading={queueLoading} 
                    onRefresh={refetchQueue} 
                />
                <RssRunControl 
                    currentRun={currentRun} 
                    onRunStatusChange={setCurrentRun} 
                />
            </div>

            <div className="space-y-4">
                <div className="flex items-center justify-between">
                    <h2 className="text-xl font-semibold tracking-tight">Start Feeds</h2>
                </div>
                
                <RssFeedTable 
                    feeds={feeds} 
                    isLoading={feedsLoading}
                    onEdit={handleEdit}
                    onDelete={(id) => {
                        if(confirm('Are you sure you want to delete this feed?')) {
                            deleteMutation.mutate(id);
                        }
                    }}
                    onToggle={(id) => toggleMutation.mutate(id)}
                    onProcess={(id) => processMutation.mutate(id)}
                    onResetFailures={(id) => resetFailuresMutation.mutate(id)}
                    onProbe={setProbingFeedId}
                />
            </div>

            <RssFeedModal 
                open={isAddOpen} 
                onClose={handleCloseModal} 
                feed={editingFeed} 
                onSuccess={handleSuccessModal}
            />

            <RssProbeModal 
                feedId={probingFeedId} 
                onClose={() => setProbingFeedId(null)} 
            />
        </div>
    );
};
