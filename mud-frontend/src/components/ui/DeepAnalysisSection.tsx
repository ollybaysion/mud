'use client';

import { useState, useRef } from 'react';
import Markdown from 'react-markdown';
import type { TrendItem } from '@/lib/types';

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
  const [progress, setProgress] = useState<{ stage: string; percent: number } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  const handleGenerate = () => {
    setLoading(true);
    setError(null);
    setProgress(null);

    const es = new EventSource(`/api/trends/${item.id}/deep-analysis/stream`);
    eventSourceRef.current = es;

    es.addEventListener('progress', (e) => {
      try {
        const data = JSON.parse(e.data);
        setProgress(data);
      } catch {
        // ignore parse errors
      }
    });

    es.addEventListener('result', (e) => {
      setAnalysis(e.data);
      setLoading(false);
      setProgress(null);
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
      setProgress(null);
      es.close();
    });
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
          <Markdown>{analysis}</Markdown>
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
            {progress ? STAGE_LABELS[progress.stage] ?? '분석 중...' : '연결 중...'}
          </>
        ) : (
          <>🔬 AI 심층 분석 생성</>
        )}
      </button>
      {loading && progress && (
        <div style={{ marginTop: '10px' }}>
          <div style={{
            width: '100%',
            height: '4px',
            background: 'var(--color-border)',
            borderRadius: '2px',
            overflow: 'hidden',
          }}>
            <div style={{
              width: `${progress.percent}%`,
              height: '100%',
              background: '#a855f7',
              borderRadius: '2px',
              transition: 'width 0.3s ease',
            }} />
          </div>
          <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginTop: '6px' }}>
            {progress.percent}% 완료
          </p>
        </div>
      )}
    </div>
  );
}
