import { useQuery } from '@tanstack/react-query';
import { Bookmark, Loader2 } from 'lucide-react';
import { Article } from '@/shared/types';
import api from '@/shared/api/client';
import { ArticleCard } from './components/ArticleCard';

interface SavedArticlesResponse {
  total: number;
  page: number;
  size: number;
  articles: Article[];
}

export const SavedArticlesPage = () => {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['saved-articles'],
    queryFn: async () => {
      const res = await api.get<SavedArticlesResponse>('/api/synthesized-articles/saved?page=0&size=20');
      return res.data;
    },
  });

  return (
    <div className="container py-8 px-4">
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
            <Bookmark className="h-5 w-5 text-primary" />
          </div>
          <h1 className="text-3xl font-bold tracking-tight" style={{ fontFamily: 'Outfit, sans-serif' }}>
            Saved Articles
          </h1>
        </div>
        <p className="text-muted-foreground ml-13">
          Articles you've bookmarked for later reading.
        </p>
      </div>

      {isLoading ? (
        <div className="flex h-64 items-center justify-center">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      ) : isError ? (
        <div className="flex h-64 flex-col items-center justify-center text-muted-foreground">
          <p>Failed to load saved articles.</p>
        </div>
      ) : !data?.articles?.length ? (
        <div className="flex h-64 flex-col items-center justify-center text-muted-foreground bg-warm-50 rounded-2xl border border-dashed border-warm-200">
          <Bookmark className="h-10 w-10 mb-3 text-warm-200" />
          <p className="font-medium">No saved articles yet</p>
          <p className="text-sm mt-1">Bookmark articles from the feed to see them here.</p>
        </div>
      ) : (
        <>
          <p className="text-sm text-muted-foreground mb-6">{data.total} article{data.total !== 1 ? 's' : ''} saved</p>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {data.articles.map((article) => (
              <ArticleCard key={article.id} article={article} isSaved={true} />
            ))}
          </div>
        </>
      )}
    </div>
  );
};
