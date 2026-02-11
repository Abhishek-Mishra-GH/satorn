import { useState, useRef, useCallback } from 'react';
import { useAuthStore } from '@/shared/store/authStore';

interface UseChatStreamProps {
  onToken: (token: string) => void;
  onProgress: (stage: string, message: string) => void;
  onComplete: (data: { sessionId: number | null; intent: string; response: string }) => void;
  onError: (error: string) => void;
}

export const useChatStream = ({ onToken, onProgress, onComplete, onError }: UseChatStreamProps) => {
  const [isStreaming, setIsStreaming] = useState(false);
  const isStreamingRef = useRef(false);
  const abortControllerRef = useRef<AbortController | null>(null);
  const { accessToken } = useAuthStore();

  const sendMessage = useCallback(async (message: string, sessionId?: number, imageUrl?: string) => {
    // Prevent duplicate concurrent streams
    if (isStreamingRef.current) return;

    // Guest mode is allowed — no token check

    isStreamingRef.current = true;
    setIsStreaming(true);
    abortControllerRef.current = new AbortController();

    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };
      // Only attach auth header if token exists
      if (accessToken) {
        headers['Authorization'] = `Bearer ${accessToken}`;
      }

      const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/chat/stream`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ message, sessionId, imageUrl }),
        signal: abortControllerRef.current.signal,
      });

      if (!response.ok) {
        const errText = await response.text().catch(() => response.statusText);
        throw new Error(`Error ${response.status}: ${errText}`);
      }

      const reader = response.body?.getReader();
      if (!reader) throw new Error('No reader available');

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // Process complete SSE frames (delimited by blank line)
        let boundaryIndex;
        while ((boundaryIndex = buffer.indexOf('\n\n')) >= 0) {
          const frame = buffer.slice(0, boundaryIndex);
          buffer = buffer.slice(boundaryIndex + 2);

          if (!frame.trim()) continue;

          const lines = frame.split('\n');
          let eventType = 'message';
          const dataLines: string[] = [];

          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventType = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
              dataLines.push(line.slice(5).trimStart());
            }
          }

          if (dataLines.length === 0) continue;
          const dataStr = dataLines.join('\n');
          if (dataStr === '[DONE]') continue;

          // Dispatch by event type
          if (eventType === 'progress') {
            try {
              const data = JSON.parse(dataStr);
              onProgress(data.stage || '', data.message || '');
            } catch {
              console.warn('Failed to parse progress event:', dataStr);
            }
          } else if (eventType === 'completed') {
            try {
              const data = JSON.parse(dataStr);
              onComplete(data);
            } catch {
              console.error('Failed to parse completed event:', dataStr);
            }
          } else if (eventType === 'error') {
            try {
              const data = JSON.parse(dataStr);
              onError(data.message || 'Unknown stream error');
            } catch {
              onError(dataStr);
            }
          } else {
            // Default: message / token — may be plain text or JSON string
            try {
              const parsed = JSON.parse(dataStr);
              if (typeof parsed === 'string') {
                onToken(parsed);
              } else if (parsed && typeof parsed === 'object' && parsed.content) {
                onToken(parsed.content);
              } else {
                onToken(String(parsed));
              }
            } catch {
              // Plain text token — this is the normal path for streaming chunks
              onToken(dataStr);
            }
          }
        }
      }
    } catch (err: any) {
      if (err.name === 'AbortError') {
        console.log('Stream aborted');
      } else {
        onError(err.message || 'Stream failed');
      }
    } finally {
      isStreamingRef.current = false;
      setIsStreaming(false);
      abortControllerRef.current = null;
    }
  }, [accessToken, onToken, onProgress, onComplete, onError]);

  const stopStream = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    isStreamingRef.current = false;
    setIsStreaming(false);
  }, []);

  return { sendMessage, stopStream, isStreaming };
};
