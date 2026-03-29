import { describe, test, expect, vi, afterEach } from 'vitest';
import { relativeTime } from './time';

describe('relativeTime', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  test('1분 미만은 "방금 전"', () => {
    const now = new Date();
    expect(relativeTime(now.toISOString())).toBe('방금 전');
  });

  test('5분 전', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-03-29T12:05:00Z'));
    expect(relativeTime('2026-03-29T12:00:00Z')).toBe('5분 전');
    vi.useRealTimers();
  });

  test('3시간 전', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-03-29T15:00:00Z'));
    expect(relativeTime('2026-03-29T12:00:00Z')).toBe('3시간 전');
    vi.useRealTimers();
  });

  test('2일 전', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-03-29T12:00:00Z'));
    expect(relativeTime('2026-03-27T12:00:00Z')).toBe('2일 전');
    vi.useRealTimers();
  });

  test('2주 전', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-03-29T12:00:00Z'));
    expect(relativeTime('2026-03-15T12:00:00Z')).toBe('2주 전');
    vi.useRealTimers();
  });

  test('30일 이상이면 절대 날짜', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-03-29T12:00:00Z'));
    const result = relativeTime('2026-02-01T12:00:00Z');
    expect(result).not.toContain('전');
    vi.useRealTimers();
  });
});
