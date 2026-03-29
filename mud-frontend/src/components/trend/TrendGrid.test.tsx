import { describe, test, expect, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { TrendGrid } from './TrendGrid';

afterEach(() => {
  cleanup();
});

describe('TrendGrid', () => {
  test('빈 배열이면 빈 상태 메시지', () => {
    render(<TrendGrid items={[]} />);
    expect(screen.getByText('조건에 맞는 트렌드가 없습니다.')).toBeTruthy();
  });

  test('빈 상태에서 안내 메시지 표시', () => {
    render(<TrendGrid items={[]} />);
    expect(screen.getByText('필터를 변경하거나 다른 카테고리를 선택해보세요.')).toBeTruthy();
  });
});
