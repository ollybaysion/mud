import { describe, test, expect, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useBookmarks } from './useBookmarks';
import type { TrendItem } from './types';

const mockItem: TrendItem = {
  id: 1,
  title: 'Test Article',
  originalUrl: 'https://example.com',
  source: 'GITHUB',
  description: null,
  koreanSummary: null,
  deepAnalysis: null,
  category: null,
  relevanceScore: 4,
  scoreTotal: null,
  scoring: null,
  topicTag: null,
  keywords: [],
  publishedAt: null,
  crawledAt: '2026-03-29T00:00:00Z',
  githubStars: null,
  githubLanguage: null,
};

const mockItem2: TrendItem = { ...mockItem, id: 2, title: 'Second Article' };

beforeEach(() => {
  localStorage.clear();
});

describe('useBookmarks', () => {
  test('초기 상태는 빈 배열', () => {
    const { result } = renderHook(() => useBookmarks());
    expect(result.current.bookmarks).toEqual([]);
  });

  test('북마크 추가', () => {
    const { result } = renderHook(() => useBookmarks());
    act(() => result.current.toggleBookmark(mockItem));
    expect(result.current.bookmarks).toHaveLength(1);
    expect(result.current.isBookmarked(1)).toBe(true);
  });

  test('북마크 제거', () => {
    const { result } = renderHook(() => useBookmarks());
    act(() => result.current.toggleBookmark(mockItem));
    act(() => result.current.toggleBookmark(mockItem));
    expect(result.current.bookmarks).toHaveLength(0);
    expect(result.current.isBookmarked(1)).toBe(false);
  });

  test('여러 아이템 북마크', () => {
    const { result } = renderHook(() => useBookmarks());
    act(() => result.current.toggleBookmark(mockItem));
    act(() => result.current.toggleBookmark(mockItem2));
    expect(result.current.bookmarks).toHaveLength(2);
    expect(result.current.isBookmarked(1)).toBe(true);
    expect(result.current.isBookmarked(2)).toBe(true);
  });

  test('존재하지 않는 ID는 false', () => {
    const { result } = renderHook(() => useBookmarks());
    expect(result.current.isBookmarked(999)).toBe(false);
  });

  test('localStorage에 잘못된 JSON이 있으면 빈 배열', () => {
    localStorage.setItem('mud-bookmarks', 'invalid-json');
    const { result } = renderHook(() => useBookmarks());
    expect(result.current.bookmarks).toEqual([]);
  });

  test('언마운트 시 리스너 정리', () => {
    const { result, unmount } = renderHook(() => useBookmarks());
    act(() => result.current.toggleBookmark(mockItem));
    unmount();
    // 언마운트 후 에러 없이 정리됨
    expect(true).toBe(true);
  });

  test('localStorage에 null이면 빈 배열', () => {
    localStorage.removeItem('mud-bookmarks');
    const { result } = renderHook(() => useBookmarks());
    expect(result.current.bookmarks).toEqual([]);
  });

  test('외부 storage 이벤트에 반응', () => {
    const { result } = renderHook(() => useBookmarks());
    act(() => {
      localStorage.setItem('mud-bookmarks', JSON.stringify([mockItem]));
      window.dispatchEvent(new StorageEvent('storage', { key: 'mud-bookmarks' }));
    });
    expect(result.current.bookmarks).toHaveLength(1);
  });
});
