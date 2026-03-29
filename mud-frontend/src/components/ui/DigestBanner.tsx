'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';

interface Props {
  periodStart: string;
  periodEnd: string;
  totalCount: number;
}

const STORAGE_KEY = 'mud-digest-banner-dismissed';

export function DigestBanner({ periodStart, periodEnd, totalCount }: Props) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const dismissed = localStorage.getItem(STORAGE_KEY);
    if (dismissed !== `${periodStart}-${periodEnd}`) {
      setVisible(true);
    }
  }, [periodStart, periodEnd]);

  const handleDismiss = () => {
    localStorage.setItem(STORAGE_KEY, `${periodStart}-${periodEnd}`);
    setVisible(false);
  };

  if (!visible) return null;

  const start = new Date(periodStart).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
  const end = new Date(periodEnd).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });

  return (
    <div style={{
      background: 'var(--color-surface)',
      border: '1px solid var(--color-accent)',
      borderRadius: '8px',
      padding: '14px 16px',
      marginBottom: '20px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: '12px',
      flexWrap: 'wrap',
    }}>
      <div>
        <div style={{ fontSize: '14px', fontWeight: 600, marginBottom: '2px' }}>
          📊 이번 주 다이제스트가 준비되었습니다
        </div>
        <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
          {start}~{end} · {totalCount.toLocaleString()}개 기사 중 AI가 선별한 핵심 트렌드
        </div>
      </div>
      <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
        <Link
          href="/digest"
          style={{
            padding: '6px 14px',
            background: 'var(--color-accent)',
            color: '#fff',
            borderRadius: '6px',
            fontSize: '13px',
            fontWeight: 500,
            whiteSpace: 'nowrap',
          }}
        >
          지금 보기 →
        </Link>
        <button
          type="button"
          onClick={handleDismiss}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--color-text-muted)',
            cursor: 'pointer',
            fontSize: '16px',
            padding: '4px',
          }}
          aria-label="배너 닫기"
        >
          ✕
        </button>
      </div>
    </div>
  );
}
