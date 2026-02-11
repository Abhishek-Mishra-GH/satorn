import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Loader2 } from 'lucide-react';
import { QuizGenerateRequest, DifficultyLevel } from '@/features/learning/types';
import { cn } from '@/shared/utils/cn';

interface QuizGeneratorProps {
  onGenerate: (config: QuizGenerateRequest) => Promise<void>;
  isGenerating: boolean;
}

export function QuizGenerator({ onGenerate, isGenerating }: QuizGeneratorProps) {
  const [category, setCategory] = useState('');
  const [difficulty, setDifficulty] = useState<DifficultyLevel>('INTERMEDIATE');
  const [questionCount, setQuestionCount] = useState(5);

  const handleStart = () => {
    onGenerate({
      category: category.trim() || undefined,
      difficulty,
      questionCount,
    });
  };

  return (
    <Card className="w-full max-w-md mx-auto">
      <CardHeader>
        <CardTitle>Start a Practice Quiz</CardTitle>
        <CardDescription>
          Generate a personalized quiz to test your knowledge.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="category">Specific Category (Optional)</Label>
          <Input
            id="category"
            placeholder="e.g. Economy, Polity"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="difficulty">Difficulty Level</Label>
          <select
            id="difficulty"
            className={cn(
              "flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            )}
            value={difficulty}
            onChange={(e) => setDifficulty(e.target.value as DifficultyLevel)}
          >
            <option value="BEGINNER">Beginner</option>
            <option value="INTERMEDIATE">Intermediate</option>
            <option value="ADVANCED">Advanced</option>
          </select>
        </div>

        <div className="space-y-2">
          <Label htmlFor="count">Number of Questions</Label>
          <Input
            type="number"
            id="count"
            min={1}
            max={20}
            value={questionCount}
            onChange={(e) => setQuestionCount(parseInt(e.target.value) || 5)}
          />
        </div>
      </CardContent>
      <CardFooter>
        <Button className="w-full" onClick={handleStart} disabled={isGenerating}>
          {isGenerating && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          Start Quiz
        </Button>
      </CardFooter>
    </Card>
  );
}
