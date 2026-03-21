'use client';

import { useState } from 'react';
import Markdown from 'react-markdown';
import type { TrendItem } from '@/lib/types';

interface Props {
  item: TrendItem;
}

export function DeepAnalysisSection({ item }: Props) {
  const [analysis, setAnalysis] = useState<string | null>(item.deepAnalysis);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleGenerate = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`/api/trends/${item.id}/deep-analysis`, {
        method: 'POST',
      });
      if (!res.ok) {
        throw new Error(`분석 요청 실패 (${res.status})`);
      }
      const data = await res.json();
      setAnalysis(data.deepAnalysis);
    } catch (e) {
      setError(e instanceof Error ? e.message : '알 수 없는 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
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
            AI 심층 분석 생성 중...
          </>
        ) : (
          <>🔬 AI 심층 분석 생성</>
        )}
      </button>
      {loading && (
        <p style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginTop: '8px' }}>
          Claude Sonnet이 분석 중입니다. 최대 30초 정도 소요될 수 있습니다.
        </p>
      )}
    </div>
  );
}
