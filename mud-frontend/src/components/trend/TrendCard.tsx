'use client';

import Link from 'next/link';
import type { TrendItem } from '@/lib/types';
import { BookmarkButton } from '@/components/ui/BookmarkButton';
import { relativeTime } from '@/lib/time';
import { useReadHistory } from '@/lib/useReadHistory';
import { SOURCE_CONFIG, SCORE_COLORS } from '@/constants/sources';

interface Props {
  item: TrendItem;
}

export function TrendCard({ item }: Props) {
  const { isRead } = useReadHistory();
  const sourceConf = SOURCE_CONFIG[item.source] ?? SOURCE_CONFIG.GITHUB;
  const scoreColor = item.relevanceScore
    ? SCORE_COLORS[item.relevanceScore]
    : '#64748b';

  const dateStr = item.publishedAt ?? item.crawledAt;
  const displayDate = relativeTime(dateStr);

  return (
    <article
      className={`trend-card ${isRead(item.id) ? 'trend-card-read' : ''}`}
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
        <Link href={`/trends/${item.id}`}>
          {item.title}
        </Link>
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
