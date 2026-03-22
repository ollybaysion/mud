import Link from 'next/link';
import type { TrendItem } from '@/lib/types';
import { BookmarkButton } from '@/components/ui/BookmarkButton';
import { sanitizeUrl } from '@/lib/url';

const SOURCE_CONFIG: Record<string, { label: string; color: string; emoji: string }> = {
  GITHUB: { label: 'GitHub', color: '#e2e8f0', emoji: '🐙' },
  HACKER_NEWS: { label: 'Hacker News', color: '#ff6600', emoji: '🧡' },
  DEV_TO: { label: 'dev.to', color: '#3b49df', emoji: '💻' },
  ARXIV: { label: 'ArXiv', color: '#b31b1b', emoji: '📄' },
  REDDIT: { label: 'Reddit', color: '#ff4500', emoji: '🔴' },
  PAPERS_WITH_CODE: { label: 'Papers With Code', color: '#21cbce', emoji: '🔬' },
  INFOQ: { label: 'InfoQ', color: '#e8372e', emoji: '📰' },
  HUGGING_FACE: { label: 'Hugging Face', color: '#ff9d00', emoji: '🤗' },
  LOBSTERS: { label: 'Lobsters', color: '#ac130d', emoji: '🦞' },
  INSIDE_JAVA: { label: 'Inside Java', color: '#f89820', emoji: '☕' },
  ISOCPP: { label: 'isocpp.org', color: '#00599c', emoji: '⚡' },
  TLDR_AI: { label: 'TLDR', color: '#1a73e8', emoji: '📧' },
  THE_NEW_STACK: { label: 'The New Stack', color: '#009bde', emoji: '☁️' },
  CNCF: { label: 'CNCF', color: '#231f20', emoji: '🐳' },
  STACKOVERFLOW_BLOG: { label: 'Stack Overflow', color: '#f48024', emoji: '📚' },
  MARTIN_FOWLER: { label: 'Martin Fowler', color: '#5b2d8e', emoji: '🏗️' },
  JETBRAINS: { label: 'JetBrains', color: '#000000', emoji: '🧠' },
  GEEKNEWS: { label: 'GeekNews', color: '#00c4b3', emoji: '🇰🇷' },
};

const SCORE_COLORS = ['', '#64748b', '#f59e0b', '#3b82f6', '#10b981', '#a855f7'];

interface Props {
  item: TrendItem;
}

export function TrendCard({ item }: Props) {
  const sourceConf = SOURCE_CONFIG[item.source] ?? SOURCE_CONFIG.GITHUB;
  const scoreColor = item.relevanceScore
    ? SCORE_COLORS[item.relevanceScore]
    : '#64748b';

  const dateStr = item.publishedAt ?? item.crawledAt;
  const displayDate = new Date(dateStr).toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric',
  });

  return (
    <article
      className="trend-card"
      style={{ '--card-accent': sourceConf.color } as React.CSSProperties}
    >
      <div className="trend-card-header">
        <span
          className="source-badge"
          style={{
            background: sourceConf.color + '22',
            color: sourceConf.color,
          }}
        >
          {sourceConf.emoji} {sourceConf.label}
        </span>

        {item.relevanceScore && (
          <span
            className="score-badge"
            style={{
              background: scoreColor + '22',
              color: scoreColor,
            }}
          >
            ★ {item.relevanceScore}/5
          </span>
        )}

        {item.githubStars !== null && item.githubStars > 0 && (
          <span className="stars-badge">⭐ {item.githubStars.toLocaleString()}</span>
        )}

        <BookmarkButton item={item} size="sm" />
      </div>

      <h3 className="trend-card-title">
        <a href={sanitizeUrl(item.originalUrl)} target="_blank" rel="noopener noreferrer">
          {item.title}
        </a>
      </h3>

      {item.koreanSummary && (
        <p className="trend-card-summary">{item.koreanSummary}</p>
      )}

      {item.keywords.length > 0 && (
        <div className="trend-card-tags">
          {item.keywords.slice(0, 5).map((kw) => (
            <span key={kw} className="tag">
              {kw}
            </span>
          ))}
        </div>
      )}

      <div className="trend-card-footer">
        {item.category && (
          <span className="category-badge">
            {item.category.emoji} {item.category.displayName}
          </span>
        )}
        <time className="trend-card-date">{displayDate}</time>
        <Link href={`/trends/${item.id}`} className="detail-link">
          자세히 →
        </Link>
      </div>
    </article>
  );
}
