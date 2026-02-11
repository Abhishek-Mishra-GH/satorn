import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { educationService } from '@/features/learning/services/learningService';
import { SkillsResponse, LearnerProfile } from '@/features/learning/types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Loader2, BookOpen, User, Trophy, BrainCircuit, ArrowRight } from 'lucide-react';

export const LearningDashboard = () => {
    const [skills, setSkills] = useState<SkillsResponse | null>(null);
    const [profile, setProfile] = useState<LearnerProfile | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadData = async () => {
            try {
                const [skillsData, profileData] = await Promise.all([
                    educationService.getSkills(),
                    educationService.getProfile()
                ]);
                setSkills(skillsData);
                setProfile(profileData);
            } catch (error) {
                console.error('Failed to load dashboard data', error);
            } finally {
                setLoading(false);
            }
        };
        loadData();
    }, []);

    if (loading) {
        return (
            <div className="flex h-[50vh] items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    return (
        <div className="container px-4 py-8 max-w-5xl mx-auto space-y-8">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Learning Hub</h1>
                    <p className="text-muted-foreground mt-2">
                        Welcome back! Continue your preparation for <span className="font-semibold text-primary">{profile?.examTrack || 'your exam'}</span>.
                    </p>
                </div>
                <Button asChild>
                   <Link to="/learning/quiz">Start Quiz</Link>
                </Button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <Card className="hover:shadow-md transition-shadow">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">Daily Goal</CardTitle>
                        <BrainCircuit className="h-4 w-4 text-muted-foreground" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{profile?.dailyStudyMinutes || 60}m</div>
                        <p className="text-xs text-muted-foreground">Target study time</p>
                    </CardContent>
                </Card>
                <Card className="hover:shadow-md transition-shadow">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">Overall Mastery</CardTitle>
                        <Trophy className="h-4 w-4 text-muted-foreground" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{skills?.overallMastery.toFixed(0)}%</div>
                         <Progress value={skills?.overallMastery} className="h-2 mt-2" />
                    </CardContent>
                </Card>
                 <Card className="hover:shadow-md transition-shadow">
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">Questions Attempted</CardTitle>
                        <BookOpen className="h-4 w-4 text-muted-foreground" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-2xl font-bold">{skills?.totalAttemptedQuestions}</div>
                         <p className="text-xs text-muted-foreground">Across all categories</p>
                    </CardContent>
                </Card>
                <Card className="hover:shadow-md transition-shadow cursor-pointer" onClick={() => window.location.href = '/learning/profile'}>
                     <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">Profile</CardTitle>
                        <User className="h-4 w-4 text-muted-foreground" />
                    </CardHeader>
                     <CardContent>
                        <div className="text-sm font-medium">Exam: {profile?.examTrack}</div>
                        <p className="text-xs text-muted-foreground mt-1 text-primary flex items-center">
                            Edit Profile <ArrowRight className="h-3 w-3 ml-1" />
                        </p>
                    </CardContent>
                </Card>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <Card className="col-span-1">
                     <CardHeader>
                        <CardTitle>Recommended Actions</CardTitle>
                        <CardDescription>Based on your recent performance</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        {skills?.needsFocusCategory && (
                             <div className="flex items-center justify-between p-4 border rounded-lg bg-orange-50/50">
                                <div>
                                    <h4 className="font-semibold text-orange-900">Focus on {skills.needsFocusCategory}</h4>
                                    <p className="text-sm text-orange-700">Your accuracy in this area is lower than others.</p>
                                </div>
                                <Button size="sm" variant="outline" className="border-orange-200 hover:bg-orange-100 text-orange-800" asChild>
                                    <Link to={`/learning/quiz`}>Practice</Link>
                                </Button>
                            </div>
                        )}
                         <div className="flex items-center justify-between p-4 border rounded-lg">
                                <div>
                                    <h4 className="font-semibold">Read New Articles</h4>
                                    <p className="text-sm text-muted-foreground">Check out curated content for your exam.</p>
                                </div>
                                <Button size="sm" variant="secondary" asChild>
                                    <Link to="/learning/feed">View Feed</Link>
                                </Button>
                            </div>
                             <div className="flex items-center justify-between p-4 border rounded-lg">
                                <div>
                                    <h4 className="font-semibold">Review Skills</h4>
                                    <p className="text-sm text-muted-foreground">Deep dive into your mastery levels.</p>
                                </div>
                                <Button size="sm" variant="ghost" asChild>
                                    <Link to="/learning/skills">View Skills</Link>
                                </Button>
                            </div>
                    </CardContent>
                </Card>

                 <Card className="col-span-1 bg-gradient-to-br from-primary/5 to-primary/10 border-primary/20">
                    <CardHeader>
                        <CardTitle>AI Tutor</CardTitle>
                         <CardDescription>Need help clarifying a concept?</CardDescription>
                    </CardHeader>
                    <CardContent className="flex flex-col items-center text-center space-y-4 py-8">
                        <div className="bg-white p-4 rounded-full shadow-sm">
                             <BrainCircuit className="h-8 w-8 text-primary" />
                        </div>
                        <p className="text-sm max-w-xs text-muted-foreground">
                            Ask our AI Tutor about any topic related to your syllabus or current affairs.
                        </p>
                       {/* We can integrate TutorChat here or link to feed */}
                       <Button asChild>
                           <Link to="/learning/feed">Go to Feed to Ask</Link> 
                       </Button>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};
