import { describe, test, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { DigestBanner } from './DigestBanner';

describe('DigestBanner', () => {
  const props = {
    periodStart: '2026-03-23',
    periodEnd: '2026-03-29',
    totalCount: 2001,
  };

  beforeEach(() => {
    localStorage.clear();
  });

  test('배너 표시', () => {
    render(<DigestBanner {...props} />);
    expect(screen.getByText('📊 이번 주 다이제스트가 준비되었습니다')).toBeTruthy();
  });

  test('기사 수 표시', () => {
    render(<DigestBanner {...props} />);
    expect(screen.getByText(/2,001/)).toBeTruthy();
  });

  test('닫기 시 localStorage 설정됨', () => {
    const { container } = render(<DigestBanner {...props} />);
    const closeBtn = container.querySelector('button[aria-label="배너 닫기"]');
    expect(closeBtn).toBeTruthy();
    act(() => {
      fireEvent.click(closeBtn!);
    });
    expect(localStorage.getItem('mud-digest-banner-dismissed')).toBe('2026-03-23-2026-03-29');
  });

  test('이미 닫은 주차는 표시 안 됨', () => {
    localStorage.setItem('mud-digest-banner-dismissed', '2026-03-23-2026-03-29');
    const { container } = render(<DigestBanner {...props} />);
    expect(container.textContent).toBe('');
  });
});
