import { describe, test, expect, beforeEach } from 'vitest';
import { render } from '@testing-library/react';
import { ReadMarker } from './ReadMarker';

beforeEach(() => {
  localStorage.clear();
});

describe('ReadMarker', () => {
  test('렌더링 시 읽음 처리', () => {
    render(<ReadMarker itemId={42} />);
    const stored = JSON.parse(localStorage.getItem('mud-read-history') || '[]');
    expect(stored).toContain(42);
  });

  test('null을 렌더링', () => {
    const { container } = render(<ReadMarker itemId={1} />);
    expect(container.innerHTML).toBe('');
  });
});
