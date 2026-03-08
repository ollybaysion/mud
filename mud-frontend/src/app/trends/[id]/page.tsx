import Link from 'next/link';
import { api } from '@/lib/api';
import { notFound } from 'next/navigation';

const SOURCE_CONFIG = {
  GITHUB: { label: 'GitHub', color: '#e2e8f0', emoji: '🐙' },
  HACKER_NEWS: { label: 'Hacker News', color: '#ff6600', emoji: '🧡' },
  DEV_TO: { label: 'dev.to', color: '#3b49df', emoji: '💻' },
  ARXIV: { label: 'ArXiv', color: '#b31b1b', emoji: '📄' },
  REDDIT: { label: 'Reddit', color: '#ff4500', emoji: '🔴' },
} as const;

const SCORE_COLORS = ['', '#64748b', '#f59e0b', '#3b82f6', '#10b981', '#a855f7'];
const SCORE_LABELS = ['', '관련성 낮음', '참고 수준', '알아두면 유용', '중요 트렌드', '즉시 적용 가능'];

interface Props {
  params: Promise<{ id: string }>;
}

export default async function TrendDetailPage({ params }: Props) {
  const { id } = await params;
  const item = await api.getTrend(Number(id)).catch(() => null);

  if (!item) notFound();

  const sourceConf = SOURCE_CONFIG[item.source] ?? SOURCE_CONFIG.GITHUB;
  const scoreColor = item.relevanceScore ? SCORE_COLORS[item.relevanceScore] : '#64748b';
  const scoreLabel = item.relevanceScore ? SCORE_LABELS[item.relevanceScore] : '';
  const dateStr = item.publishedAt ?? item.crawledAt;

  return (
    <article style={{ maxWidth: '800px' }}>
      <Link href="/trends" style={{ fontSize: '13px', color: 'var(--color-accent)', display: 'block', marginBottom: '20px' }}>
        ← 목록으로
      </Link>

      <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap' }}>
        <span className="source-badge" style={{ background: sourceConf.color + '22', color: sourceConf.color }}>
          {sourceConf.emoji} {sourceConf.label}
        </span>
        {item.category && (
          <span className="category-badge">
            {item.category.emoji} {item.category.displayName}
          </span>
        )}
        {item.relevanceScore && (
          <span className="score-badge" style={{ background: scoreColor + '22', color: scoreColor }}>
            ★ {item.relevanceScore}/5 — {scoreLabel}
          </span>
        )}
        {item.githubLanguage && (
          <span className="tag">{item.githubLanguage}</span>
        )}
      </div>

      <h1 style={{ fontSize: '22px', fontWeight: 700, lineHeight: 1.4, marginBottom: '12px' }}>
        {item.title}
      </h1>

      <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '24px' }}>
        {new Date(dateStr).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })}
        {item.githubStars !== null && item.githubStars > 0 && (
          <span style={{ marginLeft: '12px' }}>⭐ {item.githubStars.toLocaleString()} stars</span>
        )}
      </div>

      {item.koreanSummary && (
        <div style={{
          background: 'var(--color-surface)',
          border: '1px solid var(--color-border)',
          borderLeft: '3px solid var(--color-accent)',
          borderRadius: '6px',
          padding: '16px',
          marginBottom: '24px',
          fontSize: '14px',
          lineHeight: 1.8,
        }}>
          <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginBottom: '8px', fontWeight: 600 }}>
            🤖 AI 요약 (한국어)
          </div>
          {item.koreanSummary}
        </div>
      )}

      {item.description && (
        <div style={{ marginBottom: '24px' }}>
          <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '8px', fontWeight: 600 }}>
            원문 설명
          </div>
          <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', lineHeight: 1.7 }}>
            {item.description}
          </p>
        </div>
      )}

      {item.keywords.length > 0 && (
        <div style={{ marginBottom: '24px' }}>
          <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '8px', fontWeight: 600 }}>
            키워드
          </div>
          <div className="trend-card-tags">
            {item.keywords.map((kw) => (
              <span key={kw} className="tag">{kw}</span>
            ))}
          </div>
        </div>
      )}

      <a
        href={item.originalUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="page-btn"
        style={{ display: 'inline-block', padding: '10px 20px', background: 'var(--color-accent)', color: '#fff', borderRadius: '6px', fontWeight: 500 }}
      >
        원문 보기 →
      </a>
    </article>
  );
}
