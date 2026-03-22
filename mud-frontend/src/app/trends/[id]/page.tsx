import type { Metadata } from 'next';
import Link from 'next/link';
import { api } from '@/lib/api';
import { notFound } from 'next/navigation';
import { BookmarkButton } from '@/components/ui/BookmarkButton';
import { DeepAnalysisSection } from '@/components/ui/DeepAnalysisSection';
import { sanitizeUrl, stripHtml } from '@/lib/url';
import { ReadMarker } from '@/components/ui/ReadMarker';
import { SOURCE_CONFIG, SCORE_COLORS, SCORE_LABELS } from '@/constants/sources';

interface Props {
  params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const item = await api.getTrend(Number(id)).catch(() => null);

  if (!item) {
    return { title: '트렌드를 찾을 수 없습니다 | Mud' };
  }

  const description = item.koreanSummary ?? item.description?.slice(0, 160) ?? '';

  return {
    title: `${item.title} | Mud`,
    description,
    openGraph: {
      title: item.title,
      description,
      type: 'article',
      siteName: 'Mud - 기술 트렌드',
    },
    twitter: {
      card: 'summary',
      title: item.title,
      description,
    },
  };
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
      <ReadMarker itemId={item.id} />
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
            {stripHtml(item.description)}
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

      <DeepAnalysisSection item={item} />

      <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
        <a
          href={sanitizeUrl(item.originalUrl)}
          target="_blank"
          rel="noopener noreferrer"
          className="page-btn"
          style={{ display: 'inline-block', padding: '10px 20px', background: 'var(--color-accent)', color: '#fff', borderRadius: '6px', fontWeight: 500 }}
        >
          원문 보기 →
        </a>
        <BookmarkButton item={item} size="lg" />
      </div>
    </article>
  );
}
