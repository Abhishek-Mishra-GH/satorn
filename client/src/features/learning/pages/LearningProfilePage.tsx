import { useEffect, useState } from 'react';
import { ProfileForm } from '@/features/learning/components/ProfileForm';
import { educationService } from '@/features/learning/services/learningService';
import { LearnerProfile, UpdateProfileRequest } from '@/features/learning/types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';
import { Loader2 } from 'lucide-react';

export const LearningProfilePage = () => {
  const [profile, setProfile] = useState<LearnerProfile | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      const data = await educationService.getProfile();
      setProfile(data);
    } catch (error) {
      console.error('Failed to load profile', error);
      // For now, if 404, we might want to show empty form or init default?
      // Assuming backend returns 404 if not created.
      // But let's assume it returns default or empty structure if user is new,
      // or we handle error gracefully.
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async (data: UpdateProfileRequest) => {
    setSubmitting(true);
    try {
      const updated = await educationService.updateProfile(data);
      setProfile(updated);
      toast({
        title: 'Profile Updated',
        description: 'Your learning preferences have been saved.',
      });
    } catch (error) {
      console.error('Failed to update profile', error);
      toast({
        variant: 'destructive',
        title: 'Error',
        description: 'Failed to save profile. Please try again.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-[50vh] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="container px-4 py-8 max-w-3xl mx-auto space-y-8">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Learning Profile</h1>
        <p className="text-muted-foreground mt-2">
          Customize your exam preparation journey.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Profile Settings</CardTitle>
          <CardDescription>
            Manage your exam track, study goals, and difficulty preferences.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ProfileForm
            initialData={profile}
            onSubmit={handleUpdate}
            isSubmitting={submitting}
          />
        </CardContent>
      </Card>
    </div>
  );
};
