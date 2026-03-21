'use client';

import { useBookmarks } from '@/lib/useBookmarks';
import { TrendGrid } from '@/components/trend/TrendGrid';

export default function BookmarksPage() {
  const { bookmarks } = useBookmarks();

  return (
    <>
      <div style={{ marginBottom: '20px' }}>
        <h1 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '4px' }}>
          📌 저장한 글
        </h1>
        <p style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>
          {bookmarks.length > 0
            ? `${bookmarks.length}개 저장됨`
            : '저장한 글이 없습니다. 관심 있는 글의 북마크 버튼을 눌러보세요.'}
        </p>
      </div>

      <TrendGrid items={bookmarks} />
    </>
  );
}
