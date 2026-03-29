import { describe, test, expect } from 'vitest';
import { render } from '@testing-library/react';
import { CardSkeleton, GridSkeleton, FilterBarSkeleton } from './Skeleton';

describe('Skeleton components', () => {
  test('CardSkeleton 렌더링', () => {
    const { container } = render(<CardSkeleton />);
    expect(container.querySelector('.trend-card')).toBeTruthy();
    expect(container.querySelector('.skeleton-line')).toBeTruthy();
  });

  test('GridSkeleton 기본 6개 카드', () => {
    const { container } = render(<GridSkeleton />);
    expect(container.querySelectorAll('.trend-card')).toHaveLength(6);
  });

  test('GridSkeleton 커스텀 개수', () => {
    const { container } = render(<GridSkeleton count={3} />);
    expect(container.querySelectorAll('.trend-card')).toHaveLength(3);
  });

  test('FilterBarSkeleton 렌더링', () => {
    const { container } = render(<FilterBarSkeleton />);
    expect(container.querySelectorAll('.skeleton-line')).toHaveLength(2);
  });
});
