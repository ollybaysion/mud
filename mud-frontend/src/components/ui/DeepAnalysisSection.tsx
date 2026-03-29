'use client';

import dynamic from 'next/dynamic';
import remarkGfm from 'remark-gfm';
import type { TrendItem } from '@/lib/types';
import { useDeepAnalysis } from '@/lib/useDeepAnalysis';

const Markdown = dynamic(() => import('react-markdown'), { ssr: false });

const STAGE_LABELS: Record<string, string> = {
  started: '분석 준비 중...',
  analyzing: 'Claude가 분석 중...',
  done: '분석 완료!',
};

interface Props {
  item: TrendItem;
}

export function DeepAnalysisSection({ item }: Props) {
  const { analysis, loading, displayPercent, stage, elapsed, error, generate, cancel } =
    useDeepAnalysis(item.id, item.deepAnalysis);

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
          onClick={generate}
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
            onClick={cancel}
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
