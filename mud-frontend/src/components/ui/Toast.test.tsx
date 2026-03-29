import { describe, test, expect, vi, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { Toast } from './Toast';

describe('Toast', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  test('visible=true 일 때 메시지 표시', () => {
    render(<Toast message="저장되었습니다" visible={true} />);
    expect(screen.getByText('저장되었습니다')).toBeTruthy();
  });

  test('visible=false 일 때 표시 안 됨', () => {
    const { container } = render(<Toast message="test" visible={false} />);
    expect(container.textContent).toBe('');
  });

  test('2초 후 자동 숨김', () => {
    vi.useFakeTimers();
    const { container } = render(<Toast message="test" visible={true} />);
    expect(container.textContent).toBe('test');
    act(() => {
      vi.advanceTimersByTime(2100);
    });
    expect(container.textContent).toBe('');
  });
});
