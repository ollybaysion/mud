import type { Metadata } from 'next';
import Link from 'next/link';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { api } from '@/lib/api';

export const metadata: Metadata = {
  title: '주간 다이제스트',
  description: '이번 주 주요 기술 트렌드를 AI가 요약하여 제공합니다.',
};

export default async function DigestPage() {
  const report = await api.getWeeklyReport().catch((e) => {
    console.error('[digest] getWeeklyReport failed:', e);
    return null;
  });

  if (!report) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 20px' }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>📊</div>
        <h1 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '8px' }}>
          주간 다이제스트가 아직 생성되지 않았습니다
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', marginBottom: '24px' }}>
          매주 일요일에 자동 생성됩니다.
        </p>
        <Link
          href="/trends"
          style={{
            display: 'inline-block',
            padding: '10px 20px',
            background: 'var(--color-accent)',
            color: '#fff',
            borderRadius: '6px',
            fontWeight: 500,
          }}
        >
          전체 트렌드 보기
        </Link>
      </div>
    );
  }

  const periodLabel = `${new Date(report.periodStart).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })} ~ ${new Date(report.periodEnd).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })}`;

  const categoryEntries = Object.entries(report.categoryStats)
    .sort(([, a], [, b]) => b.count - a.count);

  return (
    <article style={{ maxWidth: '800px' }}>
      <Link href="/trends" style={{ fontSize: '13px', color: 'var(--color-accent)', display: 'block', marginBottom: '20px' }}>
        ← 트렌드 목록
      </Link>

      <h1 style={{ fontSize: '22px', fontWeight: 700, marginBottom: '4px' }}>
        📊 주간 트렌드 다이제스트
      </h1>
      <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', marginBottom: '24px' }}>
        {periodLabel} · 총 {report.totalCount.toLocaleString()}개 기사 분석
      </p>

      {report.aiSummary && (
        <div style={{
          background: 'var(--color-surface)',
          border: '1px solid var(--color-border)',
          borderLeft: '3px solid var(--color-accent)',
          borderRadius: '6px',
          padding: '20px',
          marginBottom: '24px',
          fontSize: '14px',
          lineHeight: 1.8,
        }}>
          <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginBottom: '12px', fontWeight: 600 }}>
            🤖 AI 주간 요약
          </div>
          <div className="deep-analysis-content">
            <Markdown remarkPlugins={[remarkGfm]}>{report.aiSummary}</Markdown>
          </div>
        </div>
      )}

      {categoryEntries.length > 0 && (
        <div style={{ marginBottom: '24px' }}>
          <h2 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '12px' }}>
            카테고리별 현황
          </h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '10px' }}>
            {categoryEntries.map(([slug, stats]) => (
              <Link
                key={slug}
                href={`/trends?category=${slug}`}
                style={{
                  background: 'var(--color-surface)',
                  border: '1px solid var(--color-border)',
                  borderRadius: '6px',
                  padding: '12px',
                  display: 'block',
                }}
              >
                <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>
                  {slug}
                </div>
                <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                  {stats.count}개 · 평균 ★{Math.round(stats.avgScore)}
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {report.highlights.length > 0 && (
        <div>
          <h2 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '12px' }}>
            🔥 이번 주 하이라이트
          </h2>
          <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {report.highlights.map((item, i) => (
              <li
                key={i}
                style={{
                  background: 'var(--color-surface)',
                  border: '1px solid var(--color-border)',
                  borderRadius: '6px',
                  padding: '12px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                  {(item.scoreTotal != null || item.relevanceScore != null) && (
                    <span style={{ color: '#a855f7', fontWeight: 600, fontSize: '12px' }}>
                      ★{String(item.scoreTotal ?? (Number(item.relevanceScore) * 20))}
                    </span>
                  )}
                  {item.id != null ? (
                    <Link href={`/trends/${String(item.id)}`} style={{ fontSize: '14px', fontWeight: 600 }}>
                      {String(item.title ?? '')}
                    </Link>
                  ) : (
                    <span style={{ fontSize: '14px', fontWeight: 600 }}>{String(item.title ?? '')}</span>
                  )}
                </div>
                {item.koreanSummary != null && (
                  <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', lineHeight: 1.6 }}>
                    {String(item.koreanSummary)}
                  </p>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </article>
  );
}
