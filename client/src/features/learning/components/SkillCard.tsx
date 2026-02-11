import { SkillStats } from '@/features/learning/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { Trophy, Target, CheckCircle2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface SkillCardProps {
  skill: SkillStats;
}

export function SkillCard({ skill }: SkillCardProps) {
  const isMastered = skill.masteryScore >= 80;
  const isBeginner = skill.masteryScore < 40;

  return (
    <Card className={cn(
      "relative overflow-hidden transition-all hover:shadow-md",
      isMastered ? "border-green-200 bg-green-50/30" : "border-gray-200"
    )}>
      {isMastered && (
        <div className="absolute top-0 right-0 p-2">
          <Trophy className="h-6 w-6 text-yellow-500" />
        </div>
      )}
      
      <CardHeader className="pb-2">
        <CardTitle className="text-lg font-medium flex items-center gap-2">
          {skill.category}
        </CardTitle>
      </CardHeader>
      
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Mastery</span>
            <span className={cn(
              "font-bold",
              isMastered ? "text-green-600" : isBeginner ? "text-orange-500" : "text-blue-600"
            )}>
              {skill.masteryScore.toFixed(0)}%
            </span>
          </div>
          <Progress value={skill.masteryScore} className={cn(
            "h-2",
            isMastered ? "bg-green-100 [&>div]:bg-green-500" : ""
          )} />
        </div>

        <div className="grid grid-cols-2 gap-4 pt-2">
          <div className="bg-white/50 p-2 rounded border text-xs">
            <div className="flex items-center gap-1 text-muted-foreground mb-1">
              <Target className="h-3 w-3" /> Attempts
            </div>
            <p className="font-semibold text-lg">{skill.attemptedQuestions}</p>
          </div>
          <div className="bg-white/50 p-2 rounded border text-xs">
            <div className="flex items-center gap-1 text-muted-foreground mb-1">
              <CheckCircle2 className="h-3 w-3" /> Accuracy
            </div>
            <p className="font-semibold text-lg">{skill.accuracyPercent.toFixed(0)}%</p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
