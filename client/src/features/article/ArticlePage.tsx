import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { 
  ArrowLeft, Calendar, ExternalLink, ShieldCheck, 
  AlertTriangle, HelpCircle, CheckCircle, FileText, 
  Clock, Share2, Bookmark, ThumbsUp, Activity,
  BrainCircuit, Copy, Sparkles, ChevronRight,
  Volume2, StopCircle 
} from 'lucide-react';
import { Article, SavedToggleResponse, LikeToggleResponse } from '@/shared/types';
import api from '@/shared/api/client';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/shared/utils/cn';
import { useAuthStore } from '@/shared/store/authStore';
import { Progress } from '@/components/ui/progress';

export const ArticlePage = () => {
  const { id } = useParams<{ id: string }>();
  const { isAuthenticated } = useAuthStore();
  const [saved, setSaved] = useState(false);
  const [liked, setLiked] = useState(false);
  const [showLoginHint, setShowLoginHint] = useState<string | null>(null);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const queryClient = useQueryClient();

  // Cleanup speech on unmount
  useEffect(() => {
    return () => {
      window.speechSynthesis.cancel();
    };
  }, []);

  const handleSpeak = () => {
    if (isSpeaking) {
      window.speechSynthesis.cancel();
      setIsSpeaking(false);
    } else {
      const text = article?.explainLikeIm5;
      if (!text) return;
      
      // Cancel any ongoing speech
      window.speechSynthesis.cancel();

      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = 'en-US';
      utterance.rate = 1;
      utterance.pitch = 1;
      
      utterance.onend = () => setIsSpeaking(false);
      utterance.onerror = () => setIsSpeaking(false);
      
      window.speechSynthesis.speak(utterance);
      setIsSpeaking(true);
    }
  };

  const { data: article, isLoading } = useQuery({
    queryKey: ['article', id],
    queryFn: async () => {
      const response = await api.get<Article>(`/api/synthesized-articles/${id}`);
      return response.data;
    },
  });

  const toggleSave = useMutation({
    mutationFn: async () => {
      if (saved) {
        const res = await api.delete<SavedToggleResponse>(`/api/synthesized-articles/${id}/save`);
        return res.data;
      } else {
        const res = await api.post<SavedToggleResponse>(`/api/synthesized-articles/${id}/save`);
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
        const res = await api.delete<LikeToggleResponse>(`/api/synthesized-articles/${id}/like`);
        return res.data;
      } else {
        const res = await api.post<LikeToggleResponse>(`/api/synthesized-articles/${id}/like`);
        return res.data;
      }
    },
    onSuccess: (data) => {
      setLiked(data.saved);
    },
  });

  const handleSaveClick = () => {
    if (!isAuthenticated) {
      setShowLoginHint('save');
      setTimeout(() => setShowLoginHint(null), 2500);
      return;
    }
    toggleSave.mutate();
  };

  const handleLikeClick = () => {
    if (!isAuthenticated) {
      setShowLoginHint('like');
      setTimeout(() => setShowLoginHint(null), 2500);
      return;
    }
    toggleLike.mutate();
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    alert("Copied to clipboard!");
  };

  if (isLoading) {
    return (
      <div className="container py-12 flex justify-center">
        <div className="animate-pulse space-y-4 w-full max-w-3xl">
          <div className="h-40 bg-muted/20 rounded-2xl w-full"></div>
          <div className="h-8 bg-muted rounded w-3/4"></div>
          <div className="space-y-4 pt-8">
            <div className="h-4 bg-muted rounded"></div>
            <div className="h-4 bg-muted rounded w-5/6"></div>
          </div>
        </div>
      </div>
    );
  }

  if (!article) {
    return <div className="container py-12 text-center">Article not found</div>;
  }

  const getVerdictDetails = (verdict: string) => {
     switch (verdict) {
       case 'TRUE': return { color: 'text-green-600 bg-green-100 border-green-200', icon: CheckCircle, label: 'True' };
       case 'MOSTLY_TRUE': return { color: 'text-emerald-600 bg-emerald-100 border-emerald-200', icon: CheckCircle, label: 'Mostly True' };
       case 'MIXED': return { color: 'text-yellow-600 bg-yellow-100 border-yellow-200', icon: HelpCircle, label: 'Mixed' };
       case 'MOSTLY_FALSE': return { color: 'text-orange-600 bg-orange-100 border-orange-200', icon: AlertTriangle, label: 'Mostly False' };
       case 'FALSE': return { color: 'text-red-600 bg-red-100 border-red-200', icon: AlertTriangle, label: 'False' };
       default: return { color: 'text-gray-600 bg-gray-100 border-gray-200', icon: HelpCircle, label: 'Unverified' };
     }
  };

  const verdictInfo = getVerdictDetails(article.verdict || 'UNVERIFIABLE');
  const VerdictIcon = verdictInfo.icon;

  // Helper to parse potentially mixed content
  const parseContent = (content?: string) => {
    if (!content) return { narrative: '', timeline: [], keyFindings: [] };
    
    // Check for JSON code block
    const jsonMatch = content.match(/```json\s*([\s\S]*?)\s*```/);
    if (jsonMatch) {
      try {
        const parsed = JSON.parse(jsonMatch[1]);
        return {
          narrative: parsed.narrative || parsed.text || '',
          timeline: parsed.timeline || [],
          keyFindings: parsed.key_findings || parsed.keyFindings || []
        };
      } catch (e) {
        console.error("Failed to parse JSON content", e);
      }
    }

    // Check if raw JSON
    if (content.trim().startsWith('{')) {
       try {
        const parsed = JSON.parse(content);
        return {
          narrative: parsed.narrative || parsed.text || '',
          timeline: parsed.timeline || [],
          keyFindings: parsed.key_findings || parsed.keyFindings || []
        };
      } catch (e) { /* ignore */ }
    }

    return { narrative: content, timeline: [], keyFindings: [] };
  };

  const { narrative: cleanNarrative, timeline: fallbackTimeline } = parseContent(article.synthesizedNarrative);

  // Use parsed data if top-level fields are empty
  const timelineEvents = (article.timelineEvents && article.timelineEvents.length > 0) 
      ? article.timelineEvents 
      : (Array.isArray(fallbackTimeline) ? fallbackTimeline : []).map((t: any) => ({ date: t.date, event: t.event }));

  const whatToWatch = article.whatToWatch || [];
  const discussionPrompts = article.discussionPrompts || [];
  const stats = article.quickStats;

  return (
    <div className="min-h-screen bg-background pb-20">
      {/* ═══════ HEADER HERO ═══════ */}
      <div className="bg-warm-50/50 border-b border-warm-100 pt-8 pb-12">
        <div className="container max-w-5xl px-4">
           <Link to="/feed" className="inline-flex items-center text-sm font-medium text-muted-foreground hover:text-primary mb-8 transition-colors">
             <ArrowLeft className="mr-2 h-4 w-4" /> Back to Feed
           </Link>

           <div className="flex flex-wrap gap-3 mb-6">
             <Badge variant="outline" className="px-3 py-1 text-sm bg-white/50 backdrop-blur-sm border-warm-200">{article.category || 'General'}</Badge>
             {article.isTrending && <Badge className="bg-blue-600 hover:bg-blue-700 px-3 py-1">Trending Topic</Badge>}
             <span className="flex items-center text-sm text-muted-foreground ml-auto bg-white/50 px-3 py-1 rounded-full border border-warm-100">
               <Clock className="h-3.5 w-3.5 mr-2 text-primary" />
               {article.readingTimeMinutes || '3'} min read
             </span>
           </div>

           <h1 className="text-3xl md:text-5xl font-extrabold tracking-tight mb-6 text-foreground bg-clip-text text-transparent bg-gradient-to-r from-foreground to-foreground/80 leading-[1.15]">
             {article.title}
           </h1>

           <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6 pt-4 border-t border-warm-200/60">
             <div className="flex items-center gap-3">
               <div className="h-12 w-12 rounded-full bg-gradient-to-br from-primary to-primary/80 text-white flex items-center justify-center shadow-lg shadow-primary/20">
                 <span className="font-bold text-lg">{article.author?.[0] || 'A'}</span>
               </div>
               <div>
                 <p className="text-sm font-bold text-foreground">{article.author || 'AI Synthesis'}</p>
                 <p className="text-xs text-muted-foreground">Source: {article.rssFeedSource || 'Multiple Sources'}</p>
               </div>
             </div>

             <div className="flex items-center gap-2 relative">
                 {/* Action Buttons */}
                 <Button variant="outline" size="sm" onClick={handleSaveClick} className={cn("rounded-full", saved && "bg-primary/10 text-primary border-primary/20")}>
                    <Bookmark className={cn("h-4 w-4 mr-2", saved && "fill-current")} /> {saved ? 'Saved' : 'Save'}
                 </Button>
                 {showLoginHint && (
                   <div className={cn(
                     "absolute -bottom-8 bg-foreground text-background text-xs px-3 py-1.5 rounded-lg shadow-lg whitespace-nowrap z-10",
                     showLoginHint === 'save' ? "right-24" : "right-12"
                   )}>
                     Login to {showLoginHint} articles
                   </div>
                 )}
                 <Button variant="outline" size="sm" onClick={handleLikeClick} className={cn("rounded-full", liked && "bg-red-50 text-red-600 border-red-200")}>
                    <ThumbsUp className={cn("h-4 w-4 mr-2", liked && "fill-current")} /> Like
                 </Button>
                 <Button variant="outline" size="sm" className="rounded-full">
                    <Share2 className="h-4 w-4 mr-2" /> Share
                 </Button>
                 
                 <Button variant="default" size="sm" className="rounded-full bg-black text-white hover:bg-black/90" asChild>
                   <a href={article.sourceUrl} target="_blank" rel="noopener noreferrer">
                     <ExternalLink className="h-4 w-4 mr-2" /> Read Original
                   </a>
                 </Button>
             </div>
           </div>
        </div>
      </div>

      <div className="container max-w-5xl px-4 py-10 grid grid-cols-1 lg:grid-cols-12 gap-10">
        
        {/* ═══════ LEFT COLUMN (MAIN) ═══════ */}
        <div className="lg:col-span-8 space-y-12">
           
           {/* Context Card */}
           {article.explainLikeIm5 && (
             <section className="bg-white rounded-2xl border border-warm-100 shadow-sm p-6 md:p-8 space-y-8">
               <div>
                 <div className="flex items-center justify-between mb-3">
                   <h3 className="flex items-center text-lg font-bold text-foreground">
                     <BrainCircuit className="h-5 w-5 text-primary mr-2" /> Quick Summary
                   </h3>
                   <Button 
                     variant="ghost" 
                     size="sm" 
                     onClick={handleSpeak}
                     className={cn("rounded-full h-8 px-3 text-muted-foreground hover:text-primary hover:bg-primary/5", isSpeaking && "text-primary bg-primary/10")}
                   >
                     {isSpeaking ? (
                       <>
                         <StopCircle className="h-3.5 w-3.5 mr-1.5 animate-pulse" /> Stop Listening
                       </>
                     ) : (
                       <>
                         <Volume2 className="h-3.5 w-3.5 mr-1.5" /> Listen
                       </>
                     )}
                   </Button>
                 </div>
                 <p className="text-lg leading-relaxed text-muted-foreground/90">
                   {article.explainLikeIm5}
                 </p>
               </div>
             </section>
           )}

           {/* Synthesized Narrative */}
           <section>
              <h2 className="text-2xl font-bold mb-6 flex items-center">
                 <FileText className="h-6 w-6 mr-3 text-primary" /> Full Story
              </h2>
              <div className="prose prose-lg prose-slate dark:prose-invert max-w-none text-muted-foreground leading-loose">
                 {cleanNarrative}
              </div>
           </section>

           {/* Timeline */}
           {timelineEvents.length > 0 && (
             <section className="bg-warm-50/50 rounded-3xl p-8 border border-warm-100">
                <h3 className="text-xl font-bold mb-8 flex items-center">
                  <Calendar className="h-5 w-5 mr-3 text-primary" /> Timeline of Events
                </h3>
                <div className="relative border-l-2 border-primary/20 ml-3 space-y-10">
                   {timelineEvents.map((evt: any, i: number) => (
                      <div key={i} className="relative pl-8">
                        <div className="absolute -left-[9px] top-1.5 h-4 w-4 rounded-full bg-white border-4 border-primary shadow-sm" />
                        <span className="inline-block px-2 py-1 rounded-md bg-white border border-warm-200 text-xs font-bold text-primary mb-2 shadow-sm">
                          {evt.date || 'Undated'}
                        </span>
                        <p className="text-base text-foreground font-medium">{evt.event}</p>
                      </div>
                   ))}
                </div>
             </section>
           )}

           {/* Discussion Prompts */}
           {discussionPrompts.length > 0 && (
             <section>
               <h3 className="text-xl font-bold mb-6">Discussion Starters</h3>
               <div className="space-y-3">
                 {discussionPrompts.map((prompt, i) => (
                   <div key={i} className="flex items-center justify-between p-4 bg-white rounded-xl border border-warm-100 shadow-sm group hover:border-primary/30 transition-all">
                     <p className="font-medium text-foreground/90 pr-4">“{prompt}”</p>
                     <Button variant="ghost" size="icon" onClick={() => copyToClipboard(prompt)} className="opacity-0 group-hover:opacity-100 transition-opacity">
                       <Copy className="h-4 w-4 text-muted-foreground" />
                     </Button>
                   </div>
                 ))}
               </div>
             </section>
           )}

           {/* Original Content Accordion */}
           <div className="border border-warm-200 rounded-xl overflow-hidden">
             <details className="group bg-warm-50/50">
                <summary className="flex cursor-pointer items-center justify-between p-5 font-medium transition-colors hover:bg-warm-100 focus:outline-none">
                  <span className="flex items-center">
                    <ExternalLink className="h-4 w-4 mr-2 text-muted-foreground" />
                    View Original Content Extraction
                  </span>
                  <ChevronRight className="h-4 w-4 text-muted-foreground transition-transform group-open:rotate-90" />
                </summary>
                <div className="p-6 bg-white border-t border-warm-200 text-sm text-muted-foreground whitespace-pre-wrap leading-relaxed max-h-[500px] overflow-y-auto font-mono">
                   {article.originalContent || "Content not available."}
                </div>
             </details>
           </div>

        </div>

        {/* ═══════ RIGHT SIDEBAR ═══════ */}
        <div className="lg:col-span-4 space-y-8">
           
           {/* Verdict Card */}
           <div className="sticky top-8 space-y-8">
             <Card className="border-0 shadow-xl shadow-primary/5 overflow-hidden ring-1 ring-black/5">
                <div className={cn("p-8 flex flex-col items-center justify-center text-center space-y-3 border-b", verdictInfo.color)}>
                   <VerdictIcon className="h-16 w-16 mb-2 opacity-90" />
                   <h2 className="text-3xl font-extrabold tracking-tight">{verdictInfo.label}</h2>
                   <div className="flex items-center gap-1.5 text-sm font-semibold opacity-80 bg-white/20 px-3 py-1 rounded-full">
                      <ShieldCheck className="h-4 w-4" />
                      {article.credibilityScore ?? 0}/100 Confidence
                   </div>
                </div>
                
                <CardContent className="p-6 bg-white space-y-6">
                  {stats && (
                    <>
                      <div className="space-y-4">
                        <div className="flex justify-between text-sm">
                          <span className="text-muted-foreground">True Claims</span>
                          <span className="font-bold text-green-600">{stats.trueClaims} ({stats.trueRatio}%)</span>
                        </div>
                        <Progress value={stats.trueRatio} className="h-2 bg-green-100" />
                        
                        <div className="flex justify-between text-sm">
                          <span className="text-muted-foreground">False Claims</span>
                          <span className="font-bold text-red-600">{stats.falseClaims} ({stats.falseRatio}%)</span>
                        </div>
                        <Progress value={stats.falseRatio} className="h-2 bg-red-100" />
                      </div>
                      <div className="pt-4 border-t grid grid-cols-2 gap-4 text-center">
                         <div className="bg-warm-50 p-3 rounded-lg">
                           <p className="text-2xl font-bold text-foreground">{stats.claimsAnalyzed}</p>
                           <p className="text-[10px] uppercase font-bold text-muted-foreground tracking-wide">Analyzed</p>
                         </div>
                         <div className="bg-warm-50 p-3 rounded-lg">
                           <p className="text-2xl font-bold text-green-600">{stats.claimsVerified}</p>
                           <p className="text-[10px] uppercase font-bold text-muted-foreground tracking-wide">Verified</p>
                         </div>
                      </div>
                    </>
                  )}
                </CardContent>
             </Card>

             {/* What to Watch */}
             {whatToWatch.length > 0 && (
               <Card className="border-l-4 border-l-amber-400 shadow-md">
                 <CardHeader>
                   <CardTitle className="flex items-center text-lg">
                     <Activity className="mr-2 h-5 w-5 text-amber-500" /> What to Watch
                   </CardTitle>
                 </CardHeader>
                 <CardContent>
                   <ul className="space-y-3">
                     {whatToWatch.map((item, i) => (
                       <li key={i} className="flex gap-3 text-sm text-foreground/80">
                         <div className="h-1.5 w-1.5 rounded-full bg-amber-400 mt-2 flex-shrink-0" />
                         {item}
                       </li>
                     ))}
                   </ul>
                 </CardContent>
               </Card>
             )}

             {/* Ask Satorn */}
             <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary via-warm-600 to-warm-700 p-6 text-white shadow-lg shadow-primary/20">
                <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full -translate-y-10 translate-x-10 blur-2xl" />
                <h3 className="text-lg font-bold mb-2 relative z-10 flex items-center">
                  <Sparkles className="mr-2 h-4 w-4" /> Dig Deeper
                </h3>
                <p className="text-sm text-white/90 mb-4 relative z-10 leading-relaxed">
                  Not sure about a specific claim? Ask our AI agent to cross-reference more sources instantly.
                </p>
                <Button variant="secondary" className="w-full relative z-10 font-semibold" asChild>
                  <Link to="/chat">Start Investigation</Link>
                </Button>
             </div>
           </div>

        </div>

      </div>
    </div>
  );
};
