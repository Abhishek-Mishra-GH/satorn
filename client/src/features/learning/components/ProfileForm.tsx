import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { LearnerProfile, UpdateProfileRequest } from '@/features/learning/types';
import { useEffect } from 'react';
import { cn } from '@/shared/utils/cn';

const profileSchema = z.object({
  examTrack: z.string().min(2, {
    message: 'Exam track must be at least 2 characters.',
  }),
  targetExamDate: z.string().refine((date) => new Date(date) > new Date(), {
    message: 'Target date must be in the future.',
  }),
  dailyStudyMinutes: z.coerce.number().min(15, {
    message: 'Daily study time must be at least 15 minutes.',
  }),
  preferredDifficulty: z.enum(['BEGINNER', 'INTERMEDIATE', 'ADVANCED']),
  weakCategories: z.string(), // accepting comma separated string
  strongCategories: z.string(),
  learningGoals: z.string().max(500, {
    message: 'Learning goals must not exceed 500 characters.',
  }),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

interface ProfileFormProps {
  initialData?: LearnerProfile;
  onSubmit: (data: UpdateProfileRequest) => Promise<void>;
  isSubmitting?: boolean;
}

export function ProfileForm({ initialData, onSubmit, isSubmitting }: ProfileFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      examTrack: '',
      targetExamDate: '',
      dailyStudyMinutes: 60,
      preferredDifficulty: 'INTERMEDIATE',
      weakCategories: '',
      strongCategories: '',
      learningGoals: '',
    },
  });

  useEffect(() => {
    if (initialData) {
      reset({
        examTrack: initialData.examTrack || '',
        targetExamDate: initialData.targetExamDate ? initialData.targetExamDate.split('T')[0] : '',
        dailyStudyMinutes: initialData.dailyStudyMinutes || 60,
        preferredDifficulty: initialData.preferredDifficulty || 'INTERMEDIATE',
        weakCategories: initialData.weakCategories ? initialData.weakCategories.join(', ') : '',
        strongCategories: initialData.strongCategories ? initialData.strongCategories.join(', ') : '',
        learningGoals: initialData.learningGoals || '',
      });
    }
  }, [initialData, reset]);

  const onFormSubmit = async (values: ProfileFormValues) => {
    await onSubmit({
      ...values,
      weakCategories: values.weakCategories.split(',').map((s) => s.trim()).filter(Boolean),
      strongCategories: values.strongCategories.split(',').map((s) => s.trim()).filter(Boolean),
    });
  };

  return (
    <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="examTrack">Exam Track</Label>
        <Input
          id="examTrack"
          placeholder="e.g. UPSC CSE, SSC CGL"
          {...register('examTrack')}
        />
        {errors.examTrack && (
          <p className="text-sm text-red-500">{errors.examTrack.message}</p>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-2">
          <Label htmlFor="targetExamDate">Target Exam Date</Label>
          <Input type="date" id="targetExamDate" {...register('targetExamDate')} />
          {errors.targetExamDate && (
            <p className="text-sm text-red-500">{errors.targetExamDate.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="dailyStudyMinutes">Daily Study Goal (minutes)</Label>
          <Input type="number" id="dailyStudyMinutes" {...register('dailyStudyMinutes')} />
          {errors.dailyStudyMinutes && (
            <p className="text-sm text-red-500">{errors.dailyStudyMinutes.message}</p>
          )}
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="preferredDifficulty">Preferred Difficulty</Label>
        <select
          id="preferredDifficulty"
          className={cn(
            "flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
          )}
          {...register('preferredDifficulty')}
        >
          <option value="BEGINNER">Beginner</option>
          <option value="INTERMEDIATE">Intermediate</option>
          <option value="ADVANCED">Advanced</option>
        </select>
        {errors.preferredDifficulty && (
          <p className="text-sm text-red-500">{errors.preferredDifficulty.message}</p>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-2">
          <Label htmlFor="weakCategories">Weak Categories (comma separated)</Label>
          <Input
            id="weakCategories"
            placeholder="Economy, Environment..."
            {...register('weakCategories')}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="strongCategories">Strong Categories (comma separated)</Label>
          <Input
            id="strongCategories"
            placeholder="Technology, Polity..."
            {...register('strongCategories')}
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="learningGoals">Learning Goals</Label>
        <Textarea
          id="learningGoals"
          placeholder="Describe what you want to achieve..."
          className="resize-none"
          {...register('learningGoals')}
        />
        {errors.learningGoals && (
          <p className="text-sm text-red-500">{errors.learningGoals.message}</p>
        )}
      </div>

      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Saving...' : 'Save Profile'}
      </Button>
    </form>
  );
}
