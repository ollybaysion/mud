import { describe, test, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BookmarkButton } from './BookmarkButton';
import type { TrendItem } from '@/lib/types';

const mockItem: TrendItem = {
  id: 1,
  title: 'Test',
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

beforeEach(() => {
  localStorage.clear();
});

describe('BookmarkButton', () => {
  test('저장 버튼 표시 (sm)', () => {
    render(<BookmarkButton item={mockItem} size="sm" />);
    expect(screen.getByTitle('저장')).toBeTruthy();
  });

  test('저장 버튼 표시 (lg)', () => {
    render(<BookmarkButton item={mockItem} size="lg" />);
    expect(screen.getByText('저장')).toBeTruthy();
  });

  test('클릭 시 저장됨 상태로 변경', () => {
    render(<BookmarkButton item={mockItem} size="lg" />);
    fireEvent.click(screen.getByRole('button'));
    expect(screen.getByText('저장됨')).toBeTruthy();
  });

  test('다시 클릭 시 저장 해제', () => {
    render(<BookmarkButton item={mockItem} size="lg" />);
    fireEvent.click(screen.getByRole('button'));
    fireEvent.click(screen.getByRole('button'));
    expect(screen.getByText('저장')).toBeTruthy();
  });
});
