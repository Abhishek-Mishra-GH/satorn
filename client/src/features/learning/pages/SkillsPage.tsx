import { useEffect, useState } from 'react';
import { educationService } from '@/features/learning/services/learningService';
import { SkillsResponse } from '@/features/learning/types';
import { SkillCard } from '@/features/learning/components/SkillCard';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { Loader2, TrendingUp, AlertTriangle, Award } from 'lucide-react';

export const SkillsPage = () => {
    const [data, setData] = useState<SkillsResponse | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadSkills();
    }, []);

    const loadSkills = async () => {
        try {
            const response = await educationService.getSkills();
            setData(response);
        } catch (error) {
            console.error('Failed to load skills', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="flex h-[50vh] items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    if (!data) {
        return <div className="p-8 text-center text-muted-foreground">Unable to load skills data.</div>;
    }

    return (
        <div className="container px-4 py-8 max-w-5xl mx-auto space-y-8">
            <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Skills Matrix</h1>
                    <p className="text-muted-foreground mt-2">
                        Track your progress across different categories and identify areas for improvement.
                    </p>
                </div>
            </div>

            {/* Overall Stats */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <Card className="bg-primary/5 border-primary/20">
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                            <Award className="h-4 w-4 text-primary" /> Overall Mastery
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="flex items-end gap-2">
                            <div className="text-4xl font-bold text-primary">
                                {data.overallMastery.toFixed(0)}%
                            </div>
                        </div>
                        <Progress value={data.overallMastery} className="h-2 mt-4" />
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                            <TrendingUp className="h-4 w-4 text-green-500" /> Strongest Area
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">
                            {data.strongestCategory || "N/A"}
                        </div>
                        <p className="text-xs text-muted-foreground mt-1">
                            Keep up the good work!
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="pb-2">
                        <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                            <AlertTriangle className="h-4 w-4 text-orange-500" /> Needs Focus
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">
                            {data.needsFocusCategory || "N/A"}
                        </div>
                        <p className="text-xs text-muted-foreground mt-1">
                            Recommended for next quiz.
                        </p>
                    </CardContent>
                </Card>
            </div>

            {/* Skills Grid */}
            <div>
                <h2 className="text-xl font-semibold mb-4">Category Breakdown</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {data.skills.map((skill) => (
                        <SkillCard key={skill.category} skill={skill} />
                    ))}
                </div>
                {data.skills.length === 0 && (
                    <div className="text-center py-12 border rounded-lg bg-gray-50 text-muted-foreground">
                        No skill data available yet. Take a quiz to start tracking your progress!
                    </div>
                )}
            </div>
        </div>
    );
};
