import { Link } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';
import { Shield, Eye, Clock, TrendingUp, AlertTriangle, CheckCircle, HelpCircle, Bookmark, ThumbsUp, ArrowRight } from 'lucide-react';
import { Article, SavedToggleResponse, LikeToggleResponse } from '@/shared/types';
import { cn } from '@/shared/utils/cn';
import { useAuthStore } from '@/shared/store/authStore';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/shared/api/client';
import { useState } from 'react';

interface ArticleCardProps {
  article: Article;
  isSaved?: boolean;
  isLiked?: boolean;
}

export const ArticleCard = ({ article, isSaved: initialSaved = false, isLiked: initialLiked = false }: ArticleCardProps) => {
  const { isAuthenticated } = useAuthStore();
  const [saved, setSaved] = useState(initialSaved);
  const [liked, setLiked] = useState(initialLiked);
  const [showLoginHint, setShowLoginHint] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const toggleSave = useMutation({
    mutationFn: async () => {
      if (saved) {
        const res = await api.delete<SavedToggleResponse>(`/api/synthesized-articles/${article.id}/save`);
        return res.data;
      } else {
        const res = await api.post<SavedToggleResponse>(`/api/synthesized-articles/${article.id}/save`);
        return res.data;
      }
    },
    onSuccess: (data) => {
      setSaved(data.saved);
      queryClient.invalidateQueries({ queryKey: ['saved-articles'] });
    },
  });

  const toggleLike = useMutation({
    mutationFn: async () => {
      if (liked) {
        const res = await api.delete<LikeToggleResponse>(`/api/synthesized-articles/${article.id}/like`);
        return res.data;
      } else {
        const res = await api.post<LikeToggleResponse>(`/api/synthesized-articles/${article.id}/like`);
        return res.data;
      }
    },
    onSuccess: (data) => {
      setLiked(data.saved); // backend returns 'saved' field for like status too
    },
  });

  const handleSaveClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (!isAuthenticated) {
      setShowLoginHint('save');
      setTimeout(() => setShowLoginHint(null), 2500);
      return;
    }

    toggleSave.mutate();
  };

  const handleLikeClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (!isAuthenticated) {
      setShowLoginHint('like');
      setTimeout(() => setShowLoginHint(null), 2500);
      return;
    }

    toggleLike.mutate();
  };

  const getVerdictColor = (verdict: string) => {
    switch (verdict) {
      case 'TRUE': return 'bg-green-50 text-green-700 border-green-200';
      case 'MOSTLY_TRUE': return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'MIXED': return 'bg-yellow-50 text-yellow-700 border-yellow-200';
      case 'MOSTLY_FALSE': return 'bg-orange-50 text-orange-700 border-orange-200';
      case 'FALSE': return 'bg-red-50 text-red-700 border-red-200';
      default: return 'bg-gray-50 text-gray-600 border-gray-200';
    }
  };

  const getVerdictIcon = (verdict: string) => {
    switch (verdict) {
      case 'TRUE':
      case 'MOSTLY_TRUE': return <CheckCircle className="h-3.5 w-3.5 mr-1" />;
      case 'MIXED': return <HelpCircle className="h-3.5 w-3.5 mr-1" />;
      case 'MOSTLY_FALSE':
      case 'FALSE': return <AlertTriangle className="h-3.5 w-3.5 mr-1" />;
      default: return <HelpCircle className="h-3.5 w-3.5 mr-1" />;
    }
  };

  return (
    <Link to={`/articles/${article.id}`} className="block h-full group">
      <div className="relative h-full flex flex-col bg-white rounded-3xl border border-warm-100 overflow-hidden transition-all duration-300 hover:shadow-xl hover:shadow-primary/5 hover:-translate-y-1">
        
        {/* Image Section */}
        <div className="relative aspect-[16/10] w-full overflow-hidden bg-warm-50">
           {article.imageUrl ? (
            <img 
              src={article.imageUrl} 
              alt={article.title}
              className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            />
           ) : (
             <div className="flex h-full w-full items-center justify-center bg-warm-100/50">
               <Shield className="h-12 w-12 text-warm-200" />
             </div>
           )}
           
           {/* Overlays */}
           <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-60" />
           
           <div className="absolute top-3 left-3 right-3 flex justify-between items-start">
             <div className={cn("inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold backdrop-blur-md border border-white/10 shadow-sm", getVerdictColor(article.verdict || 'UNVERIFIABLE').replace('bg-', 'bg-white/90 text-').replace('border-', 'border-'))}>
                {getVerdictIcon(article.verdict || 'UNVERIFIABLE')}
                {(article.verdict || 'UNVERIFIABLE').replace('_', ' ')}
             </div>

             <div className="flex gap-2">
                {article.isTrending && (
                  <span className="flex items-center justify-center h-8 w-8 rounded-full bg-primary/90 text-white backdrop-blur-sm shadow-sm" title="Trending">
                    <TrendingUp className="h-4 w-4" />
                  </span>
                )}
             </div>
           </div>

           {/* Actions positioned on image for clean look */}
           <div className="absolute bottom-3 right-3 flex gap-2">
             <button
               onClick={handleLikeClick}
               className={cn(
                 "flex h-8 w-8 items-center justify-center rounded-full backdrop-blur-md transition-all duration-200 shadow-sm border border-white/10",
                 liked
                   ? "bg-red-500 text-white"
                   : "bg-black/20 text-white/90 hover:bg-white hover:text-red-500"
               )}
             >
               <ThumbsUp className={cn("h-4 w-4", liked && "fill-current")} />
             </button>
             <button
               onClick={handleSaveClick}
               className={cn(
                 "flex h-8 w-8 items-center justify-center rounded-full backdrop-blur-md transition-all duration-200 shadow-sm border border-white/10",
                 saved
                   ? "bg-primary text-white"
                   : "bg-black/20 text-white/90 hover:bg-white hover:text-primary"
               )}
             >
               <Bookmark className={cn("h-4 w-4", saved && "fill-current")} />
             </button>
           </div>
        </div>
        
        {/* Content Section */}
        <div className="flex flex-col flex-grow p-5">
           {/* Meta */}
           <div className="flex items-center justify-between text-xs text-muted-foreground mb-3">
             <div className="flex items-center gap-2">
               <span className="font-medium text-primary bg-primary/5 px-2 py-0.5 rounded-full border border-primary/10">
                 {article.category || "General"}
               </span>
               <span className="text-warm-400">•</span>
               <span className="flex items-center">
                 <Clock className="h-3 w-3 mr-1" />
                 {article.createdAt ? formatDistanceToNow(new Date(article.createdAt), { addSuffix: true }) : 'Recently'}
               </span>
             </div>
           </div>

           {/* Title */}
           <h3 className="text-base font-bold leading-snug text-foreground mb-4 group-hover:text-primary transition-colors" style={{ fontFamily: 'Outfit, sans-serif' }}>
             {article.title}
           </h3>

           {/* Claims Summary */}
           <div className="mt-auto pt-4 flex items-center justify-between border-t border-warm-100/50">
             <div className="flex items-center gap-4 text-xs">
                <div className="flex items-center text-muted-foreground">
                   <Shield className="h-3.5 w-3.5 mr-1.5 text-warm-400" />
                   <span className="font-medium text-foreground">{article.claimsCount ?? 0}</span>
                   <span className="ml-1">Claims</span>
                </div>
                <div className="flex items-center text-muted-foreground">
                   <Eye className="h-3.5 w-3.5 mr-1.5 text-warm-400" />
                   <span>{article.viewCount ?? 0}</span>
                </div>
             </div>
             
             <div className="text-xs font-semibold text-primary flex items-center">
                Read Analysis <ArrowRight className="ml-1 h-3 w-3 transition-transform group-hover:translate-x-0.5" />
             </div>
           </div>
        </div>
        
        {/* Login Hint */}
        {showLoginHint && (
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-foreground/90 backdrop-blur text-background text-sm px-4 py-2 rounded-xl shadow-xl z-20 animate-in fade-in zoom-in duration-200">
            Login to {showLoginHint}
          </div>
        )}
      </div>
    </Link>
  );
};
