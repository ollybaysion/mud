'use client';

import { useState, useRef, useEffect, useCallback } from 'react';

const SSE_BASE = process.env.NEXT_PUBLIC_API_URL ?? '';

interface DeepAnalysisState {
  analysis: string | null;
  loading: boolean;
  displayPercent: number;
  stage: string;
  elapsed: number;
  error: string | null;
  generate: () => void;
  cancel: () => void;
}

export function useDeepAnalysis(itemId: number, initialAnalysis: string | null): DeepAnalysisState {
  const [analysis, setAnalysis] = useState<string | null>(initialAnalysis);
  const [loading, setLoading] = useState(false);
  const [serverPercent, setServerPercent] = useState(0);
  const [displayPercent, setDisplayPercent] = useState(0);
  const [stage, setStage] = useState('');
  const [elapsed, setElapsed] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const fakeTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const elapsedTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const cleanup = useCallback(() => {
    if (fakeTimerRef.current) clearInterval(fakeTimerRef.current);
    if (elapsedTimerRef.current) clearInterval(elapsedTimerRef.current);
    fakeTimerRef.current = null;
    elapsedTimerRef.current = null;
  }, []);

  useEffect(() => {
    if (!loading) return;
    elapsedTimerRef.current = setInterval(() => {
      setElapsed((prev) => prev + 1);
    }, 1000);
    return () => {
      if (elapsedTimerRef.current) clearInterval(elapsedTimerRef.current);
    };
  }, [loading]);

  useEffect(() => {
    if (!loading) return;
    fakeTimerRef.current = setInterval(() => {
      setDisplayPercent((prev) => {
        if (prev >= 90) return 90;
        return Math.min(prev + 1 + Math.random() * 2, 90);
      });
    }, 1500);
    return () => {
      if (fakeTimerRef.current) clearInterval(fakeTimerRef.current);
    };
  }, [loading]);

  useEffect(() => {
    if (serverPercent === 100) {
      cleanup();
      setDisplayPercent(100);
    } else if (serverPercent > displayPercent) {
      setDisplayPercent(serverPercent);
    }
  }, [serverPercent, displayPercent, cleanup]);

  const generate = useCallback(() => {
    setLoading(true);
    setError(null);
    setServerPercent(0);
    setDisplayPercent(0);
    setStage('');
    setElapsed(0);

    const es = new EventSource(`${SSE_BASE}/api/trends/${itemId}/deep-analysis/stream`);
    eventSourceRef.current = es;

    es.addEventListener('progress', (e) => {
      try {
        const data = JSON.parse(e.data);
        setServerPercent(data.percent);
        setStage(data.stage);
      } catch {
        // ignore
      }
    });

    es.addEventListener('result', (e) => {
      setAnalysis(e.data);
      setLoading(false);
      cleanup();
      es.close();
    });

    es.addEventListener('error', (e) => {
      if (e instanceof MessageEvent && e.data) {
        try {
          const data = JSON.parse(e.data);
          setError(data.message || '분석 중 오류가 발생했습니다.');
        } catch {
          setError('분석 중 오류가 발생했습니다.');
        }
      } else {
        setError('서버 연결이 끊어졌습니다.');
      }
      setLoading(false);
      cleanup();
      es.close();
    });
  }, [itemId, cleanup]);

  const cancel = useCallback(() => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }
    setLoading(false);
    cleanup();
    setError(null);
  }, [cleanup]);

  return { analysis, loading, displayPercent, stage, elapsed, error, generate, cancel };
}
