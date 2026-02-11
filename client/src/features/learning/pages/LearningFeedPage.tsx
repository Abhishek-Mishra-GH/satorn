import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { educationService } from '@/features/learning/services/learningService';
import { LearningArticle } from '@/features/learning/types';
import { RecommendationCard } from '@/features/learning/components/RecommendationCard';
import { Button } from '@/components/ui/button';
import { Loader2, ChevronLeft, ChevronRight, BookOpen } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const LearningFeedPage = () => {
    const [articles, setArticles] = useState<LearningArticle[]>([]);
    const [loading, setLoading] = useState(true);
    const [totalPages, setTotalPages] = useState(0);
    const [searchParams, setSearchParams] = useSearchParams();
    const navigate = useNavigate();

    const page = parseInt(searchParams.get('page') || '0', 10);
    const size = 10;

    useEffect(() => {
        loadRecommendations();
    }, [page]);

    const loadRecommendations = async () => {
        setLoading(true);
        try {
            const response = await educationService.getRecommendations(page, size);
            setArticles(response.articles);
            setTotalPages(response.totalPages);
        } catch (error) {
            console.error('Failed to load recommendations', error);
        } finally {
            setLoading(false);
        }
    };

    const handlePageChange = (newPage: number) => {
        setSearchParams({ page: newPage.toString() });
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const handleStudyClick = (articleId: number) => {
        // Navigate to dedicated article study page or open modal
        // For now, let's assume we navigate to the standard article page with a query param?
        // Or maybe just the standard article page is fine, and we add education features there later.
        navigate(`/articles/${articleId}?mode=study`);
    };

    return (
        <div className="container px-4 py-8 max-w-5xl mx-auto space-y-8">
            <div className="flex justify-between items-end">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
                        <BookOpen className="h-8 w-8 text-primary" />
                        Your Learning Feed
                    </h1>
                    <p className="text-muted-foreground mt-2">
                        Curated articles based on your latest quiz performance and weak areas.
                    </p>
                </div>
            </div>

            {loading ? (
                <div className="flex h-64 items-center justify-center">
                    <Loader2 className="h-8 w-8 animate-spin text-primary" />
                </div>
            ) : (
                <>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 gap-6">
                        {articles.map((article) => (
                            <RecommendationCard
                                key={article.id}
                                article={article}
                                onClick={() => handleStudyClick(article.id)}
                            />
                        ))}
                    </div>

                    {articles.length === 0 && (
                        <div className="text-center py-12 text-muted-foreground">
                            No recommendations found. Try updating your profile or taking a quiz!
                        </div>
                    )}

                    {totalPages > 1 && (
                        <div className="flex justify-center items-center gap-4 mt-8">
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handlePageChange(Math.max(0, page - 1))}
                                disabled={page === 0}
                            >
                                <ChevronLeft className="h-4 w-4 mr-2" />
                                Previous
                            </Button>
                            <span className="text-sm font-medium">
                                Page {page + 1} of {totalPages}
                            </span>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handlePageChange(Math.min(totalPages - 1, page + 1))}
                                disabled={page >= totalPages - 1}
                            >
                                Next
                                <ChevronRight className="h-4 w-4 ml-2" />
                            </Button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};
