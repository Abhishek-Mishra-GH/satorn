import { useState } from 'react';
import { QuizSession as QuizSessionType, QuizAnswer } from '@/features/learning/types';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Progress } from '@/components/ui/progress';
import { Loader2, ArrowRight, CheckCircle } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface QuizSessionProps {
  session: QuizSessionType;
  onSubmit: (answers: QuizAnswer[]) => Promise<void>;
  isSubmitting: boolean;
}

export function QuizSession({ session, onSubmit, isSubmitting }: QuizSessionProps) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<number, string>>({});

  const question = session.questions[currentIndex];
  const totalQuestions = session.questions.length;
  const progress = ((currentIndex + 1) / totalQuestions) * 100;

  const handleOptionSelect = (value: string) => {
    setAnswers((prev) => ({
      ...prev,
      [question.id]: value,
    }));
  };

  const handleNext = () => {
    if (currentIndex < totalQuestions - 1) {
      setCurrentIndex(currentIndex + 1);
    } else {
      handleSubmit();
    }
  };

  const handleSubmit = () => {
    const formattedAnswers: QuizAnswer[] = Object.entries(answers).map(([questionId, selectedOption]) => ({
      questionId: parseInt(questionId),
      selectedOption,
    }));
    onSubmit(formattedAnswers);
  };

  const isCurrentAnswered = !!answers[question.id];

  return (
    <div className="w-full max-w-2xl mx-auto space-y-6">
      <div className="flex justify-between items-center text-sm text-muted-foreground">
        <span>Question {currentIndex + 1} of {totalQuestions}</span>
        <span>{session.difficulty} • {session.focusCategory}</span>
      </div>
      
      <Progress value={progress} className="h-2" />

      <Card>
        <CardHeader>
          <CardTitle className="text-xl font-medium leading-normal">
            {question.question}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {Object.entries(question.options).map(([key, value]) => (
              <div 
                key={key} 
                className={cn(
                  "flex items-center space-x-2 border rounded-lg p-4 cursor-pointer transition-colors",
                  answers[question.id] === key ? "bg-primary/5 border-primary" : "hover:bg-accent"
                )}
                onClick={() => handleOptionSelect(key)}
              >
                <div className={cn(
                  "h-4 w-4 rounded-full border border-primary flex items-center justify-center",
                  answers[question.id] === key ? "bg-primary" : "bg-transparent"
                )}>
                  {answers[question.id] === key && <div className="h-2 w-2 rounded-full bg-primary-foreground" />}
                </div>
                <Label className="flex-1 cursor-pointer font-normal">
                  <span className="font-semibold mr-2">{key}.</span> {value}
                </Label>
              </div>
            ))}
          </div>
        </CardContent>
        <CardFooter className="flex justify-end pt-4">
          <Button onClick={handleNext} disabled={!isCurrentAnswered || isSubmitting}>
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Submitting...
              </>
            ) : currentIndex === totalQuestions - 1 ? (
              <>
                Finish Quiz <CheckCircle className="ml-2 h-4 w-4" />
              </>
            ) : (
              <>
                Next Question <ArrowRight className="ml-2 h-4 w-4" />
              </>
            )}
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
