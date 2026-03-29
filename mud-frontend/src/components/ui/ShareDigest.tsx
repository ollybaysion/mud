'use client';

import { useState } from 'react';

interface Props {
  periodStart: string;
  periodEnd: string;
  highlights: Array<Record<string, unknown>>;
}

export function ShareDigest({ periodStart, periodEnd, highlights }: Props) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    const start = new Date(periodStart).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
    const end = new Date(periodEnd).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });

    const topItems = highlights.slice(0, 3).map((item) => {
      const cat = item.categorySlug ? `${String(item.categorySlug)}` : '';
      return `${cat ? cat + ' — ' : ''}${String(item.title ?? '')}`;
    });

    const text = [
      `⚗️ Mud 주간 트렌드 (${start}~${end})`,
      ...topItems.map((t) => `• ${t}`),
      `전체 보기 → ${window.location.origin}/digest`,
    ].join('\n');

    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback
    }
  };

  return (
    <button
      type="button"
      onClick={handleCopy}
      style={{
        padding: '8px 16px',
        background: copied ? '#10b981' : 'var(--color-surface)',
        color: copied ? '#fff' : 'var(--color-text-muted)',
        border: '1px solid var(--color-border)',
        borderRadius: '6px',
        fontSize: '13px',
        cursor: 'pointer',
        transition: 'background 0.2s',
      }}
    >
      {copied ? '✓ 복사됨' : '📋 공유용 텍스트 복사'}
    </button>
  );
}
