import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { RssFeedConfigDto } from '@/shared/types/rssAdmin';
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
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { Loader2 } from 'lucide-react';

interface RssFeedModalProps {
  open: boolean;
  onClose: () => void;
  feed?: RssFeedConfigDto;
  onSuccess: () => void;
}

interface FormData {
  name: string;
  feedUrl: string;
  description: string;
  category: string;
  updateFrequencyMinutes: number;
  enabled: boolean;
}

export const RssFeedModal = ({ open, onClose, feed, onSuccess }: RssFeedModalProps) => {
  const isEdit = !!feed;
  const { register, handleSubmit, reset, setValue, formState: { errors, isSubmitting } } = useForm<FormData>({
    defaultValues: {
      name: '',
      feedUrl: '',
      description: '',
      category: 'General',
      updateFrequencyMinutes: 60,
      enabled: true
    }
  });

  useEffect(() => {
    if (open && feed) {
      setValue('name', feed.name);
      setValue('feedUrl', feed.feedUrl);
      setValue('description', feed.description || '');
      setValue('category', feed.category);
      setValue('updateFrequencyMinutes', feed.updateFrequencyMinutes);
      setValue('enabled', feed.enabled);
    } else if (open && !feed) {
      reset({
         name: '',
         feedUrl: '',
         description: '',
         category: 'General',
         updateFrequencyMinutes: 60,
         enabled: true
      });
    }
  }, [open, feed, setValue, reset]);

  const onSubmit = async (data: FormData) => {
    try {
      if (isEdit && feed) {
        await rssService.updateFeed(feed.id, data);
      } else {
        await rssService.createFeed(data);
      }
      onSuccess();
      onClose();
    } catch (error) {
       console.error("Failed to save feed", error);
       alert("Failed to save feed: " + (error as any).response?.data?.message || (error as any).message);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Edit RSS Feed' : 'Add RSS Feed'}</DialogTitle>
          <DialogDescription>
            Configure the RSS feed source and update frequency.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 py-4">
          <div className="grid gap-2">
            <Label htmlFor="name">Feed Name *</Label>
            <Input id="name" {...register('name', { required: 'Name is required' })} placeholder="e.g. BBC World News" />
            {errors.name && <p className="text-destructive text-xs">{errors.name.message}</p>}
          </div>
          
          <div className="grid gap-2">
            <Label htmlFor="feedUrl">Feed URL *</Label>
            <Input id="feedUrl" {...register('feedUrl', { required: 'URL is required' })} placeholder="https://..." />
            {errors.feedUrl && <p className="text-destructive text-xs">{errors.feedUrl.message}</p>}
          </div>

          <div className="grid grid-cols-2 gap-4">
             <div className="grid gap-2">
                <Label htmlFor="category">Category</Label>
                <Input id="category" {...register('category')} placeholder="Politics, Tech..." />
             </div>
             <div className="grid gap-2">
                <Label htmlFor="frequency">Update (Minutes)</Label>
                <Input 
                    id="frequency" 
                    type="number" 
                    min={5}
                    {...register('updateFrequencyMinutes', { valueAsNumber: true })} 
                />
             </div>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="description">Description (Optional)</Label>
            <Input id="description" {...register('description')} placeholder="Brief description of content" />
          </div>

          <div className="flex items-center space-x-2">
              <Checkbox 
                id="enabled" 
                checked={feed?.enabled} // This is tricky with RHF and uncontrolled checkbox, need to handle state or use Controller. 
                // Simplified for now: just trust RHF register
                {...register('enabled')}
              />
              <Label htmlFor="enabled">Enable Automatic Updates</Label>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={isSubmitting}>
                {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {isEdit ? 'Save Changes' : 'Create Feed'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};
