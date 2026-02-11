import { RssFeedConfigDto } from '@/shared/types/rssAdmin';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { 
  MoreHorizontal, 
  Edit, 
  Trash, 
  Play, 
  Activity, 
  AlertTriangle, 
  RefreshCw 
} from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { formatDistanceToNow } from 'date-fns';

interface RssFeedTableProps {
  feeds: RssFeedConfigDto[];
  isLoading: boolean;
  onEdit: (feed: RssFeedConfigDto) => void;
  onDelete: (id: number) => void;
  onToggle: (id: number) => void;
  onProcess: (id: number) => void;
  onResetFailures: (id: number) => void;
  onProbe: (id: number) => void;
}

export const RssFeedTable = ({ 
  feeds, 
  isLoading, 
  onEdit, 
  onDelete, 
  onToggle, 
  onProcess,
  onResetFailures,
  onProbe
}: RssFeedTableProps) => {

  if (isLoading) {
      return <div className="p-8 text-center text-muted-foreground">Loading feeds...</div>;
  }

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-[250px]">Feed Details</TableHead>
            <TableHead>Category</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Last Checked</TableHead>
            <TableHead className="text-right">Stats</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {feeds.length === 0 ? (
            <TableRow>
                <TableCell colSpan={6} className="h-24 text-center">
                    No RSS feeds configured.
                </TableCell>
            </TableRow>
          ) : feeds.map((feed) => (
            <TableRow key={feed.id}>
              <TableCell>
                <div className="flex flex-col">
                    <span className="font-medium">{feed.name}</span>
                    <a href={feed.feedUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-muted-foreground hover:underline truncate max-w-[200px]">
                        {feed.feedUrl}
                    </a>
                </div>
              </TableCell>
              <TableCell>{feed.category}</TableCell>
              <TableCell>
                <div className="flex items-center gap-2">
                    <Badge variant={feed.enabled ? 'outline' : 'secondary'}>
                        {feed.enabled ? 'Enabled' : 'Disabled'}
                    </Badge>
                    {feed.consecutiveFailures > 0 && (
                        <Badge variant="destructive" className="items-center gap-1">
                             <AlertTriangle className="h-3 w-3" /> {feed.consecutiveFailures}
                        </Badge>
                    )}
                </div>
              </TableCell>
              <TableCell>
                 {feed.lastChecked ? (
                     <div className="flex flex-col">
                        <span className="text-sm">{formatDistanceToNow(new Date(feed.lastChecked), { addSuffix: true })}</span>
                        {feed.lastError && (
                            <span className="text-xs text-red-500 truncate max-w-[150px]" title={feed.lastError}>
                                {feed.lastError}
                            </span>
                        )}
                     </div>
                 ) : (
                     <span className="text-muted-foreground text-sm">Never</span>
                 )}
              </TableCell>
              <TableCell className="text-right">
                <div className="flex flex-col items-end">
                    <span className="font-medium">{feed.articlesProcessed}</span>
                    <span className="text-xs text-muted-foreground">processed</span>
                </div>
              </TableCell>
              <TableCell className="text-right">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" className="h-8 w-8 p-0">
                      <span className="sr-only">Open menu</span>
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuLabel>Actions</DropdownMenuLabel>
                    <DropdownMenuItem onClick={() => onToggle(feed.id)}>
                        {feed.enabled ? <span className="text-muted-foreground">Disable Feed</span> : <span className="font-medium">Enable Feed</span>}
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => onEdit(feed)}>
                        <Edit className="mr-2 h-4 w-4" /> Edit Configuration
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={() => onProcess(feed.id)}>
                        <Play className="mr-2 h-4 w-4" /> Process Now
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => onProbe(feed.id)}>
                        <Activity className="mr-2 h-4 w-4" /> Probe Connection
                    </DropdownMenuItem>
                    {feed.consecutiveFailures > 0 && (
                        <DropdownMenuItem onClick={() => onResetFailures(feed.id)}>
                            <RefreshCw className="mr-2 h-4 w-4" /> Reset Failures
                        </DropdownMenuItem>
                    )}
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={() => onDelete(feed.id)} className="text-destructive focus:text-destructive">
                        <Trash className="mr-2 h-4 w-4" /> Delete Feed
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};
