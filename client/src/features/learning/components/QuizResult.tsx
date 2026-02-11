import { QuizResult as QuizResultType } from '@/features/learning/types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { CheckCircle, XCircle, AlertCircle, RefreshCcw } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface QuizResultProps {
  result: QuizResultType;
  onRetry: () => void;
}

export function QuizResult({ result, onRetry }: QuizResultProps) {
  return (
    <div className="w-full max-w-3xl mx-auto space-y-8 pb-12">
      <div className="text-center space-y-4">
        <h1 className="text-3xl font-bold">Quiz Completed!</h1>
        <div className="flex justify-center items-center gap-6">
          <div className="text-center">
            <p className="text-sm text-muted-foreground">Score</p>
            <p className="text-4xl font-extrabold text-primary">{result.scorePercent.toFixed(0)}%</p>
          </div>
          <div className="text-center">
            <p className="text-sm text-muted-foreground">Correct</p>
            <p className="text-4xl font-extrabold text-green-600">
              {result.correctAnswers}/{result.totalQuestions}
            </p>
          </div>
        </div>
      </div>

      <div className="space-y-6">
        <h2 className="text-xl font-semibold">Detailed Analysis</h2>
        {result.results.map((item, index) => (
          <Card key={item.questionId} className={cn("border-l-4", item.isCorrect ? "border-l-green-500" : "border-l-red-500")}>
            <CardHeader className="pb-2">
              <div className="flex items-start gap-3">
                {item.isCorrect ? (
                  <CheckCircle className="h-5 w-5 text-green-500 mt-1 flex-shrink-0" />
                ) : (
                  <XCircle className="h-5 w-5 text-red-500 mt-1 flex-shrink-0" />
                )}
                <div className="space-y-1">
                  <CardTitle className="text-base font-normal leading-relaxed">
                    {index + 1}. {item.question}
                  </CardTitle>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4 pl-12">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                <div className={cn("p-3 rounded-md border", item.isCorrect ? "bg-green-50 border-green-200" : "bg-red-50 border-red-200")}>
                  <p className="font-semibold mb-1">Your Answer:</p>
                  <p>{item.selectedOption}</p>
                </div>
                {!item.isCorrect && (
                  <div className="p-3 rounded-md border bg-green-50 border-green-200">
                    <p className="font-semibold mb-1">Correct Answer:</p>
                    <p>{item.correctOption}</p>
                  </div>
                )}
              </div>
              
              <Alert variant="default" className="bg-blue-50/50 border-blue-200 text-blue-800">
                <AlertCircle className="h-4 w-4 text-blue-600" />
                <AlertTitle className="text-blue-700 font-semibold mb-1">Explanation</AlertTitle>
                <AlertDescription className="text-blue-700/90 leading-relaxed">
                  {item.explanation}
                </AlertDescription>
              </Alert>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="flex justify-center pt-4">
        <Button size="lg" onClick={onRetry}>
          <RefreshCcw className="mr-2 h-4 w-4" />
          Take Another Quiz
        </Button>
      </div>
    </div>
  );
}
