'use client';

import { useState, useRef, useEffect, useCallback } from 'react';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import type { TrendItem } from '@/lib/types';

const SSE_BASE = process.env.NEXT_PUBLIC_API_URL ?? '';

const STAGE_LABELS: Record<string, string> = {
  started: '분석 준비 중...',
  analyzing: 'Claude가 분석 중...',
  done: '분석 완료!',
};

interface Props {
  item: TrendItem;
}

export function DeepAnalysisSection({ item }: Props) {
  const [analysis, setAnalysis] = useState<string | null>(item.deepAnalysis);
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
        const increment = 1 + Math.random() * 2;
        return Math.min(prev + increment, 90);
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

  const handleGenerate = () => {
    setLoading(true);
    setError(null);
    setServerPercent(0);
    setDisplayPercent(0);
    setStage('');
    setElapsed(0);

    const es = new EventSource(`${SSE_BASE}/api/trends/${item.id}/deep-analysis/stream`);
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
  };

  const handleCancel = () => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }
    setLoading(false);
    cleanup();
    setError(null);
  };

  const formatElapsed = (s: number) => {
    if (s < 60) return `${s}초`;
    return `${Math.floor(s / 60)}분 ${s % 60}초`;
  };

  if (analysis) {
    return (
      <div style={{
        background: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
        borderLeft: '3px solid #a855f7',
        borderRadius: '6px',
        padding: '20px',
        marginBottom: '24px',
        fontSize: '14px',
        lineHeight: 1.8,
      }}>
        <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginBottom: '12px', fontWeight: 600 }}>
          🔬 AI 심층 분석
        </div>
        <div className="deep-analysis-content">
          <Markdown remarkPlugins={[remarkGfm]}>{analysis}</Markdown>
        </div>
      </div>
    );
  }

  return (
    <div style={{ marginBottom: '24px' }}>
      {error && (
        <div style={{
          background: '#fef2f2',
          color: '#dc2626',
          padding: '10px 14px',
          borderRadius: '6px',
          fontSize: '13px',
          marginBottom: '10px',
        }}>
          {error}
        </div>
      )}
      <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
        <button
          type="button"
          onClick={handleGenerate}
          disabled={loading}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '10px 20px',
            background: loading ? 'var(--color-border)' : '#a855f722',
            color: loading ? 'var(--color-text-muted)' : '#a855f7',
            border: '1px solid #a855f744',
            borderRadius: '6px',
            fontWeight: 500,
            fontSize: '14px',
            cursor: loading ? 'not-allowed' : 'pointer',
          }}
        >
          {loading ? (
            <>
              <span className="deep-analysis-spinner" />
              {stage ? STAGE_LABELS[stage] ?? '분석 중...' : '연결 중...'}
            </>
          ) : (
            <>🔬 AI 심층 분석 생성</>
          )}
        </button>
        {loading && (
          <button
            type="button"
            onClick={handleCancel}
            style={{
              padding: '10px 16px',
              background: 'none',
              color: 'var(--color-text-muted)',
              border: '1px solid var(--color-border)',
              borderRadius: '6px',
              fontSize: '13px',
              cursor: 'pointer',
            }}
          >
            취소
          </button>
        )}
      </div>
      {!loading && (
        <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginTop: '6px' }}>
          AI가 상세 분석을 생성합니다 (약 1~2분 소요)
        </p>
      )}
      {loading && (
        <div style={{ marginTop: '10px' }}>
          <div style={{
            width: '100%',
            height: '4px',
            background: 'var(--color-border)',
            borderRadius: '2px',
            overflow: 'hidden',
          }}>
            <div style={{
              width: `${displayPercent}%`,
              height: '100%',
              background: '#a855f7',
              borderRadius: '2px',
              transition: 'width 0.5s ease',
            }} />
          </div>
          <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginTop: '6px' }}>
            {Math.round(displayPercent)}% 완료 · {formatElapsed(elapsed)} 경과
            {displayPercent < 90 && ' · 보통 1~2분 소요됩니다'}
          </p>
        </div>
      )}
    </div>
  );
}
