'use client';

import { useBookmarks } from '@/lib/useBookmarks';
import type { TrendItem } from '@/lib/types';

interface Props {
  item: TrendItem;
  size?: 'sm' | 'lg';
}

export function BookmarkButton({ item, size = 'sm' }: Props) {
  const { isBookmarked, toggleBookmark } = useBookmarks();
  const saved = isBookmarked(item.id);

  return (
    <button
      type="button"
      className={`bookmark-btn ${saved ? 'bookmarked' : ''}`}
      onClick={(e) => {
        e.preventDefault();
        e.stopPropagation();
        toggleBookmark(item);
      }}
      title={saved ? '저장 취소' : '저장'}
      aria-label={saved ? '저장 취소' : '저장'}
    >
      {saved ? (
        <svg width={size === 'lg' ? 20 : 16} height={size === 'lg' ? 20 : 16} viewBox="0 0 24 24" fill="currentColor">
          <path d="M5 2h14a1 1 0 0 1 1 1v19.143a.5.5 0 0 1-.766.424L12 18.03l-7.234 4.536A.5.5 0 0 1 4 22.143V3a1 1 0 0 1 1-1z" />
        </svg>
      ) : (
        <svg width={size === 'lg' ? 20 : 16} height={size === 'lg' ? 20 : 16} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M5 2h14a1 1 0 0 1 1 1v19.143a.5.5 0 0 1-.766.424L12 18.03l-7.234 4.536A.5.5 0 0 1 4 22.143V3a1 1 0 0 1 1-1z" />
        </svg>
      )}
      {size === 'lg' && (
        <span style={{ marginLeft: '6px' }}>{saved ? '저장됨' : '저장'}</span>
      )}
    </button>
  );
}
