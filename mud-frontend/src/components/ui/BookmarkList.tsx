'use client';

import { useBookmarks } from '@/lib/useBookmarks';
import { TrendGrid } from '@/components/trend/TrendGrid';

export function BookmarkList() {
  const { bookmarks } = useBookmarks();

  return (
    <>
      <p style={{ fontSize: '13px', color: 'var(--color-text-muted)', marginBottom: '20px' }}>
        {bookmarks.length > 0
          ? `${bookmarks.length}개 저장됨`
          : '저장한 글이 없습니다. 관심 있는 글의 북마크 버튼을 눌러보세요.'}
      </p>
      <TrendGrid items={bookmarks} />
    </>
  );
}
