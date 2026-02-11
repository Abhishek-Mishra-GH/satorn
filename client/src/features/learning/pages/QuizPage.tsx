import { useState } from 'react';
import { QuizGenerator } from '@/features/learning/components/QuizGenerator';
import { QuizSession } from '@/features/learning/components/QuizSession';
import { QuizResult } from '@/features/learning/components/QuizResult';
import { educationService } from '@/features/learning/services/learningService';
import { QuizGenerateRequest, QuizSession as QuizSessionType, QuizAnswer, QuizResult as QuizResultType } from '@/features/learning/types';
import { useToast } from '@/hooks/use-toast';

type QuizStep = 'GENERATOR' | 'SESSION' | 'RESULT';

export const QuizPage = () => {
    const [step, setStep] = useState<QuizStep>('GENERATOR');
    const [session, setSession] = useState<QuizSessionType | null>(null);
    const [result, setResult] = useState<QuizResultType | null>(null);
    const [loading, setLoading] = useState(false);
    const { toast } = useToast();

    const handleGenerate = async (config: QuizGenerateRequest) => {
        setLoading(true);
        try {
            const newSession = await educationService.generateQuiz(config);
            setSession(newSession);
            setStep('SESSION');
        } catch (error) {
            console.error('Failed to generate quiz', error);
            toast({
                variant: 'destructive',
                title: 'Error',
                description: 'Failed to generate quiz. Please try again.',
            });
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (answers: QuizAnswer[]) => {
        if (!session) return;
        setLoading(true);
        try {
            const quizResult = await educationService.submitQuiz({
                quizSessionId: session.quizSessionId,
                answers,
            });
            setResult(quizResult);
            setStep('RESULT');
        } catch (error) {
            console.error('Failed to submit quiz', error);
            toast({
                variant: 'destructive',
                title: 'Error',
                description: 'Failed to submit quiz. Please try again.',
            });
        } finally {
            setLoading(false);
        }
    };

    const handleRetry = () => {
        setSession(null);
        setResult(null);
        setStep('GENERATOR');
    };

    return (
        <div className="container px-4 py-8 max-w-4xl mx-auto min-h-[80vh] flex flex-col justify-center">
            {step === 'GENERATOR' && (
                <div className="space-y-8">
                    <div className="text-center">
                        <h1 className="text-3xl font-bold tracking-tight">Adaptive Quiz</h1>
                        <p className="text-muted-foreground mt-2">
                            Test your knowledge with AI-generated questions based on verified news.
                        </p>
                    </div>
                    <QuizGenerator onGenerate={handleGenerate} isGenerating={loading} />
                </div>
            )}

            {step === 'SESSION' && session && (
                <QuizSession
                    session={session}
                    onSubmit={handleSubmit}
                    isSubmitting={loading}
                />
            )}

            {step === 'RESULT' && result && (
                <QuizResult result={result} onRetry={handleRetry} />
            )}
        </div>
    );
};
