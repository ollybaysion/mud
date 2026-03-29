import { describe, test, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ShareDigest } from './ShareDigest';

describe('ShareDigest', () => {
  const props = {
    periodStart: '2026-03-23',
    periodEnd: '2026-03-29',
    highlights: [
      { title: 'Article 1', categorySlug: 'ai-ml' },
      { title: 'Article 2', categorySlug: 'devops' },
    ],
  };

  test('공유 버튼 표시', () => {
    render(<ShareDigest {...props} />);
    expect(screen.getByRole('button')).toBeTruthy();
  });

  test('클릭 시 클립보드 복사', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });
    render(<ShareDigest {...props} />);
    fireEvent.click(screen.getByRole('button'));
    await vi.waitFor(() => {
      expect(writeText).toHaveBeenCalled();
    });
  });
});
