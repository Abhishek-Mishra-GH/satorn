import { useState, useRef, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Send, Loader2, Bot, User, Plus, MessageSquare, StopCircle, Lock, LogIn, Sparkles, ShieldCheck, Globe, LinkIcon } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '@/shared/store/authStore';
import api from '@/shared/api/client';
import { ChatSession, ChatMessage as ChatMessageType } from '@/shared/types';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area'; 
import { cn } from '@/shared/utils/cn';
import { useChatStream } from './hooks/useChatStream';
import { Badge } from '@/components/ui/badge';

// Known phrases from backend that indicate login is required
const LOGIN_REQUIRED_PHRASES = [
  'requires login',
  'login to verify',
  'log in to verify',
  'saved to your account',
  'sign in to',
];

function containsLoginPrompt(text: string): boolean {
  const lower = text.toLowerCase();
  return LOGIN_REQUIRED_PHRASES.some(phrase => lower.includes(phrase));
}

const QUICK_PROMPTS = [
  { icon: LinkIcon, label: 'Verify a link', prompt: 'Can you verify this article for me: ' },
  { icon: Globe, label: 'Latest news check', prompt: 'What are the most credible stories trending right now?' },
  { icon: ShieldCheck, label: 'Fact check a claim', prompt: 'Is it true that ' },
  { icon: Sparkles, label: 'Explain a topic', prompt: 'Give me an unbiased analysis of ' },
];

export const ChatPage = () => {
    const [currentSessionId, setCurrentSessionId] = useState<number | null>(null);
    const [messages, setMessages] = useState<ChatMessageType[]>([]);
    const [input, setInput] = useState('');
    const [progress, setProgress] = useState<{stage: string, message: string} | null>(null);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const queryClient = useQueryClient();
    const { user, isAuthenticated } = useAuthStore();

    // Fetch Sessions — only for authenticated users
    const { data: sessions, isLoading: sessionsLoading } = useQuery({
        queryKey: ['chat-sessions'],
        queryFn: async () => {
            const res = await api.get<ChatSession[]>('/api/chat/sessions?limit=20');
            if (Array.isArray(res.data)) return res.data;
            return (res.data as any).sessions || [];
        },
        enabled: isAuthenticated,
    });

    // Fetch history when session changes
    useEffect(() => {
        if (currentSessionId && isAuthenticated) {
            api.get<ChatMessageType[]>(`/api/chat/sessions/${currentSessionId}`)
                .then(res => {
                    if (Array.isArray(res.data)) {
                        setMessages(res.data);
                    } else {
                        setMessages((res.data as any).messages || []);
                    }
                })
                .catch(err => console.error("Failed to load session", err));
        } else if (!currentSessionId) {
            setMessages([]);
        }
    }, [currentSessionId, isAuthenticated]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, progress]);

    const { sendMessage, stopStream, isStreaming } = useChatStream({
        onToken: (token) => {
            setMessages(prev => {
                const last = prev[prev.length - 1];
                if (last && last.role === 'assistant' && last.isStreaming) {
                    return [...prev.slice(0, -1), { ...last, content: last.content + token }];
                }
                return [...prev, { role: 'assistant', content: token, isStreaming: true }];
            });
            setProgress(null);
        },
        onProgress: (stage, message) => {
            setProgress({ stage, message });
        },
        onComplete: (data) => {
            setMessages(prev => {
                const last = prev[prev.length - 1];
                if (last && last.role === 'assistant' && last.isStreaming) {
                    return [...prev.slice(0, -1), { ...last, isStreaming: false }];
                }
                return prev;
            });
            setProgress(null);
            if (isAuthenticated && !currentSessionId && data.sessionId) {
                setCurrentSessionId(data.sessionId);
                queryClient.invalidateQueries({ queryKey: ['chat-sessions'] });
            }
        },
        onError: (err) => {
            setMessages(prev => {
                const last = prev[prev.length - 1];
                if (last && last.role === 'assistant' && last.isStreaming) {
                    return [...prev.slice(0, -1), { ...last, content: last.content + `\n\n⚠️ Error: ${err}`, isStreaming: false }];
                }
                return [...prev, { role: 'assistant', content: `⚠️ Error: ${err}`, isStreaming: false }];
            });
            setProgress(null);
        }
    });

    const handleSend = async (e?: React.FormEvent) => {
        e?.preventDefault();
        if (!input.trim() || isStreaming) return;

        const userMsg: ChatMessageType = { role: 'user', content: input, timestamp: new Date().toISOString() };
        const msgToSend = input;
        setInput('');

        setMessages(prev => [
            ...prev,
            userMsg,
            { role: 'assistant', content: '', isStreaming: true },
        ]);

        await sendMessage(msgToSend, currentSessionId || undefined);
    };

    const handleQuickPrompt = (prompt: string) => {
        setInput(prompt);
        inputRef.current?.focus();
    };

    const handleNewSession = () => {
        setCurrentSessionId(null);
        setMessages([]);
        setInput('');
    };

    const showEmptyState = messages.length === 0 && !currentSessionId;

    return (
        <div className="flex bg-gradient-to-br from-warm-50/30 via-white to-warm-50/20 h-[calc(100vh-65px)]"> 
            {/* ═══ Sidebar ═══ */}
            <div className="w-72 border-r border-warm-100 hidden md:flex flex-col bg-white/80 backdrop-blur-sm">
                <div className="p-4 border-b border-warm-100">
                    <Button onClick={handleNewSession} className="w-full rounded-xl bg-primary hover:bg-primary/90 text-white shadow-sm shadow-primary/20" size="sm">
                        <Plus className="mr-2 h-4 w-4" /> New Conversation
                    </Button>
                </div>

                {isAuthenticated ? (
                    <ScrollArea className="flex-1">
                        <div className="p-3">
                            <p className="text-[10px] uppercase tracking-wider text-muted-foreground font-semibold px-2 mb-2">Recent</p>
                            <div className="flex flex-col gap-0.5">
                                {sessionsLoading && (
                                    <div className="flex items-center justify-center py-8">
                                        <Loader2 className="h-5 w-5 animate-spin text-primary" />
                                    </div>
                                )}
                                {sessions?.length === 0 && !sessionsLoading && (
                                    <p className="text-xs text-muted-foreground text-center py-8 px-4">
                                        No conversations yet. Start one above!
                                    </p>
                                )}
                                {sessions?.map((session: ChatSession) => (
                                    <button
                                        key={session.id}
                                        className={cn(
                                            "flex items-start gap-3 w-full text-left px-3 py-2.5 rounded-xl transition-all duration-200 cursor-pointer",
                                            currentSessionId === session.id
                                                ? "bg-primary/10 text-primary"
                                                : "hover:bg-warm-50 text-foreground/70"
                                        )}
                                        onClick={() => setCurrentSessionId(session.id)}
                                    >
                                        <MessageSquare className="h-4 w-4 shrink-0 mt-0.5" />
                                        <div className="min-w-0 flex-1">
                                            <p className="text-xs font-medium truncate">
                                                {session.title || `Session #${session.id}`}
                                            </p>
                                            <p className="text-[10px] text-muted-foreground mt-0.5">
                                                {session.createdAt && new Date(session.createdAt).toLocaleDateString()}
                                            </p>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        </div>
                    </ScrollArea>
                ) : (
                    <div className="flex-1 flex flex-col items-center justify-center px-6 text-center">
                        <div className="relative mb-4">
                            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-warm-100 to-warm-200">
                                <Lock className="h-6 w-6 text-primary" />
                            </div>
                        </div>
                        <p className="text-sm font-medium text-foreground/80 mb-1" style={{ fontFamily: 'Outfit, sans-serif' }}>
                            Save your sessions
                        </p>
                        <p className="text-xs text-muted-foreground mb-5 leading-relaxed">
                            Login to save and revisit your chat history.
                        </p>
                        <Button asChild variant="outline" size="sm" className="rounded-full border-warm-200 text-xs">
                            <Link to="/login">
                                <LogIn className="mr-2 h-3 w-3" /> Login
                            </Link>
                        </Button>
                    </div>
                )}

                {/* Sidebar footer */}
                <div className="p-4 border-t border-warm-100">
                    <div className="bg-warm-50 rounded-xl p-3">
                        <div className="flex items-center gap-2 mb-1.5">
                            <ShieldCheck className="h-3.5 w-3.5 text-primary" />
                            <span className="text-[10px] font-semibold text-foreground/70 uppercase tracking-wider">Powered by AI</span>
                        </div>
                        <p className="text-[10px] text-muted-foreground leading-relaxed">
                            SATORN's AI may occasionally make mistakes. Always verify critical information.
                        </p>
                    </div>
                </div>
            </div>

            {/* ═══ Main Chat Area ═══ */}
            <div className="flex-1 flex flex-col min-w-0">
                {showEmptyState ? (
                    /* ═══ Empty State ═══ */
                    <div className="flex-1 flex flex-col items-center justify-center px-4">
                        <div className="max-w-2xl w-full text-center">
                            {/* Logo + Greeting */}
                            <div className="mb-8">
                                <div className="relative inline-flex">
                                    <div className="absolute inset-0 bg-primary/10 rounded-3xl blur-xl scale-150" />
                                    <div className="relative flex h-20 w-20 items-center justify-center rounded-3xl bg-gradient-to-br from-primary to-warm-600 shadow-xl shadow-primary/25 mx-auto mb-5">
                                        <Bot className="h-10 w-10 text-white" />
                                    </div>
                                </div>
                                <h2 className="text-2xl sm:text-3xl font-bold tracking-tight mb-2" style={{ fontFamily: 'Outfit, sans-serif' }}>
                                    {user?.username ? `Hey ${user.username}, what should we verify?` : 'What should we verify?'}
                                </h2>
                                <p className="text-muted-foreground max-w-md mx-auto">
                                    Paste a link, ask about a claim, or pick a suggestion below.
                                    {!isAuthenticated && (
                                        <> You're chatting as a guest — <Link to="/login" className="text-primary underline font-medium">login</Link> to save sessions.</>
                                    )}
                                </p>
                            </div>

                            {/* Quick Prompt Cards */}
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-w-lg mx-auto mb-10">
                                {QUICK_PROMPTS.map((item) => (
                                    <button
                                        key={item.label}
                                        onClick={() => handleQuickPrompt(item.prompt)}
                                        className="group flex items-center gap-3 text-left p-4 rounded-2xl border border-warm-100 bg-white hover:border-primary/30 hover:shadow-md hover:shadow-warm-100/50 transition-all duration-300 cursor-pointer"
                                    >
                                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-warm-50 group-hover:bg-primary/10 transition-colors flex-shrink-0">
                                            <item.icon className="h-5 w-5 text-muted-foreground group-hover:text-primary transition-colors" />
                                        </div>
                                        <span className="text-sm font-medium text-foreground/70 group-hover:text-foreground transition-colors">
                                            {item.label}
                                        </span>
                                    </button>
                                ))}
                            </div>

                            {/* Input in empty state */}
                            <div className="max-w-xl mx-auto">
                                <form onSubmit={handleSend} className="relative">
                                    <input
                                        ref={inputRef}
                                        type="text"
                                        placeholder="Paste a link or ask anything..."
                                        value={input}
                                        onChange={(e) => setInput(e.target.value)}
                                        disabled={isStreaming}
                                        className="w-full h-14 pl-6 pr-14 rounded-2xl border border-warm-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary shadow-lg shadow-warm-100/30 placeholder:text-muted-foreground/50 transition-all"
                                    />
                                    <button
                                        type="submit"
                                        disabled={!input.trim() || isStreaming}
                                        className="absolute right-2 top-1/2 -translate-y-1/2 h-10 w-10 rounded-xl bg-primary text-white flex items-center justify-center hover:bg-primary/90 disabled:opacity-40 disabled:cursor-not-allowed transition-all shadow-sm"
                                    >
                                        <Send className="h-4 w-4" />
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>
                ) : (
                    /* ═══ Messages View ═══ */
                    <>
                        <ScrollArea className="flex-1 p-4">
                            <div className="max-w-3xl mx-auto space-y-5 py-4">
                                {messages.map((msg, i) => (
                                    <div key={i} className={cn("flex gap-3", msg.role === 'user' ? "flex-row-reverse" : "flex-row")}>
                                        <div className={cn(
                                            "w-9 h-9 rounded-xl flex items-center justify-center shrink-0 shadow-sm", 
                                            msg.role === 'user'
                                                ? "bg-gradient-to-br from-primary to-warm-600 text-white"
                                                : "bg-white border border-warm-100 text-muted-foreground"
                                        )}>
                                            {msg.role === 'user' ? <User className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
                                        </div>
                                        <div className={cn(
                                            "relative rounded-2xl px-5 py-3.5 max-w-[80%] text-sm leading-relaxed",
                                            msg.role === 'user'
                                                ? "bg-gradient-to-br from-primary to-warm-600 text-white rounded-tr-md shadow-md shadow-primary/10"
                                                : "bg-white border border-warm-100 rounded-tl-md shadow-sm"
                                        )}>
                                            {msg.role === 'assistant' && msg.isStreaming && !msg.content ? (
                                                <div className="flex items-center gap-2">
                                                    <Loader2 className="h-4 w-4 animate-spin text-primary" />
                                                    <span className="text-xs text-muted-foreground">Thinking...</span>
                                                </div>
                                            ) : (
                                                <>
                                                    <div className="whitespace-pre-wrap">{msg.content}</div>
                                                    {msg.role === 'assistant' && !msg.isStreaming && !isAuthenticated && containsLoginPrompt(msg.content) && (
                                                        <div className="mt-3 pt-3 border-t border-warm-100">
                                                            <Link
                                                                to="/login"
                                                                className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary text-white text-xs font-semibold hover:bg-primary/90 transition-colors shadow-sm"
                                                            >
                                                                <LogIn className="h-3 w-3" /> Login to verify links
                                                            </Link>
                                                        </div>
                                                    )}
                                                </>
                                            )}
                                        </div>
                                    </div>
                                ))}

                                {/* Progress indicator */}
                                {isStreaming && progress && (
                                    <div className="flex justify-start ml-12">
                                        <Badge variant="outline" className="animate-pulse bg-white border-warm-200 shadow-sm">
                                            <Loader2 className="mr-2 h-3 w-3 animate-spin text-primary" />
                                            {progress.message || progress.stage}
                                        </Badge>
                                    </div>
                                )}
                                <div ref={messagesEndRef} />
                            </div>
                        </ScrollArea>

                        {/* Input area (conversation mode) */}
                        <div className="p-4 border-t border-warm-100 bg-white/80 backdrop-blur-sm">
                            <div className="max-w-3xl mx-auto">
                                <form onSubmit={handleSend} className="relative">
                                    <input
                                        type="text"
                                        placeholder="Ask a follow-up or paste another link..."
                                        value={input}
                                        onChange={(e) => setInput(e.target.value)}
                                        disabled={isStreaming}
                                        className="w-full h-12 pl-5 pr-24 rounded-2xl border border-warm-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary placeholder:text-muted-foreground/50 transition-all"
                                    />
                                    <div className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-1.5">
                                        {isStreaming ? (
                                            <button
                                                type="button"
                                                onClick={stopStream}
                                                className="h-8 w-8 rounded-lg bg-red-500 text-white flex items-center justify-center hover:bg-red-600 transition-colors"
                                            >
                                                <StopCircle className="h-4 w-4" />
                                            </button>
                                        ) : (
                                            <button
                                                type="submit"
                                                disabled={!input.trim()}
                                                className="h-8 w-8 rounded-lg bg-primary text-white flex items-center justify-center hover:bg-primary/90 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                                            >
                                                <Send className="h-3.5 w-3.5" />
                                            </button>
                                        )}
                                    </div>
                                </form>
                            </div>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};
