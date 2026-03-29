import type { Metadata } from 'next';
import { BookmarkList } from '@/components/ui/BookmarkList';

export const metadata: Metadata = {
  title: '저장한 글',
  description: '북마크한 기술 트렌드 목록',
};

export default function BookmarksPage() {
  return (
    <>
      <h1 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '4px' }}>
        📌 저장한 글
      </h1>
      <BookmarkList />
    </>
  );
}
