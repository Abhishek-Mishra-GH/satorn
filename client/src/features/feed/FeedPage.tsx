import { useState } from 'react';
import { useInfiniteQuery } from '@tanstack/react-query';
import { Search, Loader2, TrendingUp, CheckCircle, Archive, SlidersHorizontal, Sparkles, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { articleService } from '@/shared/api/articleService';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { ArticleCard } from './components/ArticleCard';
import { useDebounce } from '@/shared/hooks/useDebounce';

type FeedTab = 'all' | 'trending' | 'credible' | 'category';

export const FeedPage = () => {
  const [activeTab, setActiveTab] = useState<FeedTab>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const debouncedSearch = useDebounce(searchQuery, 500);

  const { 
    data, 
    isLoading, 
    isError, 
    fetchNextPage, 
    hasNextPage, 
    isFetchingNextPage 
  } = useInfiniteQuery({
    queryKey: ['articles', activeTab, debouncedSearch, selectedCategory],
    queryFn: async ({ pageParam = 0 }) => {
      const params = { page: pageParam, size: 12 };

      if (debouncedSearch) {
        return articleService.searchArticles({ ...params, query: debouncedSearch });
      } else if (activeTab === 'trending') {
        return articleService.getTrendingArticles(params);
      } else if (activeTab === 'credible') {
        return articleService.getTopCredibleArticles(params);
      } else if (activeTab === 'category' && selectedCategory) {
        return articleService.getArticlesByCategory(selectedCategory, params);
      }
      
      return articleService.getSynthesizedArticles(params);
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) => {
      // If the API returns fewer items than requested size, we've reached the end
      if (!lastPage.articles || lastPage.articles.length < 12) {
        return undefined;
      }
      // If the API returns explicit page info, use it, otherwise increment
      return (lastPage.page !== undefined) ? lastPage.page + 1 : allPages.length;
    },
  });

  const allArticles = data?.pages.flatMap(page => page.articles || []) || [];

  const categories = ['Politics', 'Technology', 'Economy', 'Health', 'Science', 'Entertainment', 'Sports'];

  return (
    <div className="container py-8 pl-4 pr-4">
      <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold tracking-tight" style={{ fontFamily: 'Outfit, sans-serif' }}>News Feed</h1>
          <p className="text-muted-foreground">Verified synthesized articles from trusted sources.</p>
        </div>
        <div className="relative w-full md:w-96">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            type="search"
            placeholder="Search topics, claims, or entities..."
            className="pl-10 rounded-full border-warm-200 focus:ring-primary/30"
            value={searchQuery}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      <div className="flex flex-col md:flex-row gap-6">
        {/* Sidebar Filters */}
        <div className="w-full md:w-64 flex-shrink-0 space-y-4">
           <div className="bg-white rounded-2xl border border-warm-100 p-1.5 grid grid-cols-2 md:grid-cols-1 gap-1 shadow-sm">
             <Button 
               variant={activeTab === 'all' && !debouncedSearch ? "secondary" : "ghost"} 
               className={`justify-start rounded-xl ${activeTab === 'all' && !debouncedSearch ? 'bg-warm-100 text-primary font-semibold' : ''}`}
               onClick={() => setActiveTab('all')}
             >
               <Archive className="mr-2 h-4 w-4" /> All News
             </Button>
             <Button 
               variant={activeTab === 'trending' ? "secondary" : "ghost"} 
               className={`justify-start rounded-xl ${activeTab === 'trending' ? 'bg-warm-100 text-primary font-semibold' : ''}`}
               onClick={() => { setActiveTab('trending'); setSearchQuery(''); }}
             >
               <TrendingUp className="mr-2 h-4 w-4" /> Trending
             </Button>
             <Button 
               variant={activeTab === 'credible' ? "secondary" : "ghost"} 
               className={`justify-start rounded-xl ${activeTab === 'credible' ? 'bg-warm-100 text-primary font-semibold' : ''}`}
               onClick={() => { setActiveTab('credible'); setSearchQuery(''); }}
             >
               <CheckCircle className="mr-2 h-4 w-4" /> Top Credible
             </Button>
           </div>
           
           <div className="bg-white rounded-2xl border border-warm-100 p-4 shadow-sm">
             <h3 className="font-semibold mb-3 flex items-center text-foreground/80" style={{ fontFamily: 'Outfit, sans-serif' }}>
               <SlidersHorizontal className="mr-2 h-4 w-4 text-primary" /> Categories
             </h3>
             <div className="flex gap-2 overflow-x-auto pb-2 md:flex-wrap md:pb-0 -mx-2 px-2 md:mx-0 md:px-0 scrollbar-hide">
               {categories.map(cat => (
                 <button
                   key={cat}
                   onClick={() => { 
                     setSelectedCategory(cat); 
                     setActiveTab('category');
                     setSearchQuery('');
                   }}
                   className={`px-4 py-1.5 rounded-full text-xs font-medium transition-all duration-200 cursor-pointer whitespace-nowrap flex-shrink-0 ${
                     selectedCategory === cat && activeTab === 'category'
                       ? 'bg-primary text-white shadow-sm shadow-primary/20'
                       : 'bg-warm-50 text-foreground/70 border border-warm-200 hover:border-primary/40 hover:text-primary'
                   }`}
                 >
                 {cat}
                 </button>
               ))}
             </div>
           </div>

           {/* Ask SATORN CTA Card */}
           {/* Mobile Compact Banner */}
           <div className="md:hidden relative overflow-hidden rounded-xl bg-gradient-to-r from-primary via-warm-600 to-warm-700 p-3 shadow-md flex items-center justify-between mb-4 md:mb-0">
             <div className="flex items-center gap-3">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/20 backdrop-blur-sm">
                  <Sparkles className="h-4 w-4 text-white" />
                </div>
                <span className="font-bold text-white text-sm">Ask SATORN</span>
             </div>
             <Link to="/chat" className="text-xs font-semibold text-white bg-white/20 px-3 py-1.5 rounded-full backdrop-blur-sm hover:bg-white/30 transition-colors flex items-center">
               Debunk <ArrowRight className="inline h-3 w-3 ml-1" />
             </Link>
           </div>

           {/* Desktop Large Banner */}
           <div className="hidden md:block relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary via-warm-600 to-warm-700 p-5 shadow-lg shadow-primary/15">
             <div className="absolute top-0 right-0 w-24 h-24 bg-white/10 rounded-full -translate-y-8 translate-x-8 blur-xl" />
             <div className="absolute bottom-0 left-0 w-16 h-16 bg-white/10 rounded-full translate-y-6 -translate-x-4 blur-lg" />
             <div className="relative z-10">
               <div className="flex items-center gap-2 mb-3">
                 <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-white/20 backdrop-blur-sm">
                   <Sparkles className="h-5 w-5 text-white" />
                 </div>
                 <h3 className="text-base font-bold text-white" style={{ fontFamily: 'Outfit, sans-serif' }}>
                   Ask SATORN
                 </h3>
               </div>
               <p className="text-sm text-white/85 leading-relaxed mb-4">
                 Verify any article or ask about the latest news. Our AI will fact-check it for you.
               </p>
               <Link
                 to="/chat"
                 className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white text-primary text-sm font-semibold hover:bg-white/90 transition-colors shadow-sm"
               >
                 Try Debunk <ArrowRight className="h-3.5 w-3.5" />
               </Link>
             </div>
           </div>
        </div>

        {/* Main Feed Grid */}
        <div className="flex-1 pb-10">
          {isLoading ? (
            <div className="flex h-64 items-center justify-center">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : isError ? (
            <div className="flex h-64 flex-col items-center justify-center text-muted-foreground">
              <p>Failed to load articles. Please try again.</p>
              <Button variant="outline" className="mt-4 rounded-full border-warm-200" onClick={() => window.location.reload()}>Retry</Button>
            </div>
          ) : allArticles.length === 0 ? (
            <div className="flex h-64 flex-col items-center justify-center text-muted-foreground bg-warm-50 rounded-2xl border border-dashed border-warm-200">
              <p>No articles found matching your criteria.</p>
            </div>
          ) : (
            <>
              <div className="grid gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
                {allArticles.map((article) => (
                  <ArticleCard key={article.id} article={article} />
                ))}
              </div>
              
              {hasNextPage && (
                <div className="flex justify-center mt-10">
                  <Button 
                    onClick={() => fetchNextPage()} 
                    disabled={isFetchingNextPage}
                    variant="outline"
                    className="rounded-full px-8 py-6 border-warm-200 text-muted-foreground hover:text-primary hover:border-primary/50"
                  >
                    {isFetchingNextPage ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Loading more...
                      </>
                    ) : (
                      'Load More Articles'
                    )}
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};
