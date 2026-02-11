import { LearningArticle } from '@/features/learning/types';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ExternalLink, Star } from 'lucide-react';
import { TutorChat } from '@/features/learning/components/TutorChat';

interface RecommendationCardProps {
  article: LearningArticle;
  onClick?: () => void;
}

export function RecommendationCard({ article, onClick }: RecommendationCardProps) {
  return (
    <Card className="hover:shadow-lg transition-shadow duration-200">
      <CardHeader>
        <div className="flex justify-between items-start gap-4">
          <div className="space-y-1">
            <Badge variant="outline" className="mb-2">
              {article.category}
            </Badge>
            <CardTitle className="text-xl line-clamp-2">
              {article.title}
            </CardTitle>
            <CardDescription className="flex items-center gap-2 text-xs">
              <span>{new Date(article.createdAt).toLocaleDateString()}</span>
              <span>•</span>
              <span className={getVerdictColor(article.verdict)}>
                {article.verdict.replace('_', ' ')}
              </span>
            </CardDescription>
          </div>
          {article.credibilityScore && (
            <div className="flex flex-col items-center justify-center p-2 bg-green-50 rounded-lg border border-green-100 min-w-[60px]">
              <span className="text-lg font-bold text-green-700">{article.credibilityScore}%</span>
              <span className="text-[10px] text-green-600 uppercase">Trusted</span>
            </div>
          )}
        </div>
      </CardHeader>
      <CardContent>
        {article.whyRecommended && article.whyRecommended.length > 0 && (
          <div className="bg-blue-50/50 p-3 rounded-md border border-blue-100">
            <p className="text-xs font-semibold text-blue-700 mb-1 flex items-center gap-1">
              <Star className="h-3 w-3 fill-blue-700" />
              Why Recommended
            </p>
            <ul className="list-disc list-inside space-y-0.5">
              {article.whyRecommended.map((reason, idx) => (
                <li key={idx} className="text-xs text-blue-600/90">
                  {reason}
                </li>
              ))}
            </ul>
          </div>
        )}
      </CardContent>
      <CardFooter className="flex flex-col sm:flex-row gap-3 sm:items-center justify-between bg-gray-50/50 py-3">
        <Button variant="ghost" size="sm" asChild className="text-muted-foreground hover:text-primary w-full sm:w-auto justify-center">
          <a href={article.sourceUrl} target="_blank" rel="noopener noreferrer">
            <ExternalLink className="mr-2 h-3.5 w-3.5" />
            Read Source
          </a>
        </Button>
        <div className="flex gap-2 w-full sm:w-auto">
           <div className="flex-1 sm:flex-none">
             <TutorChat 
                contextArticleId={article.id}
                trigger={
                  <Button variant="outline" size="sm" className="w-full">
                    Ask Tutor
                  </Button>
                } 
              />
           </div>
            <Button size="sm" onClick={onClick} className="flex-1 sm:flex-none">
              Analyze & Study
            </Button>
        </div>
      </CardFooter>
    </Card>
  );
}

function getVerdictColor(verdict: string) {
  switch (verdict) {
    case 'TRUE':
    case 'MOSTLY_TRUE':
      return 'text-green-600 font-medium';
    case 'FALSE':
    case 'MOSTLY_FALSE':
      return 'text-red-600 font-medium';
    case 'Mixture':
      return 'text-yellow-600 font-medium';
    default:
      return 'text-gray-600';
  }
}
