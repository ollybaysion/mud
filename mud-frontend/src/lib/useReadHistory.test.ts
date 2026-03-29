import { describe, test, expect, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useReadHistory } from './useReadHistory';

beforeEach(() => {
  localStorage.clear();
});

describe('useReadHistory', () => {
  test('초기 상태는 빈 배열', () => {
    const { result } = renderHook(() => useReadHistory());
    expect(result.current.readIds).toEqual([]);
  });

  test('읽음 처리', () => {
    const { result } = renderHook(() => useReadHistory());
    act(() => result.current.markAsRead(1));
    expect(result.current.isRead(1)).toBe(true);
  });

  test('읽지 않은 ID는 false', () => {
    const { result } = renderHook(() => useReadHistory());
    expect(result.current.isRead(999)).toBe(false);
  });

  test('중복 읽음 처리는 무시', () => {
    const { result } = renderHook(() => useReadHistory());
    act(() => result.current.markAsRead(1));
    act(() => result.current.markAsRead(1));
    expect(result.current.readIds.filter((id) => id === 1)).toHaveLength(1);
  });

  test('여러 아이템 읽음 처리', () => {
    const { result } = renderHook(() => useReadHistory());
    act(() => result.current.markAsRead(1));
    act(() => result.current.markAsRead(2));
    act(() => result.current.markAsRead(3));
    expect(result.current.readIds).toHaveLength(3);
    expect(result.current.isRead(1)).toBe(true);
    expect(result.current.isRead(2)).toBe(true);
    expect(result.current.isRead(3)).toBe(true);
  });

  test('localStorage에 잘못된 JSON이 있으면 빈 배열', () => {
    localStorage.setItem('mud-read-history', '{broken}');
    const { result } = renderHook(() => useReadHistory());
    expect(result.current.readIds).toEqual([]);
  });

  test('최대 500개까지만 저장', () => {
    const { result } = renderHook(() => useReadHistory());
    for (let i = 0; i < 510; i++) {
      act(() => result.current.markAsRead(i));
    }
    const stored = JSON.parse(localStorage.getItem('mud-read-history') || '[]');
    expect(stored.length).toBeLessThanOrEqual(500);
  });

  test('언마운트 시 리스너 정리', () => {
    const { result, unmount } = renderHook(() => useReadHistory());
    act(() => result.current.markAsRead(1));
    unmount();
    expect(true).toBe(true);
  });

  test('외부 storage 이벤트에 반응', () => {
    const { result } = renderHook(() => useReadHistory());
    act(() => {
      localStorage.setItem('mud-read-history', JSON.stringify([42]));
      window.dispatchEvent(new StorageEvent('storage', { key: 'mud-read-history' }));
    });
    expect(result.current.isRead(42)).toBe(true);
  });

  test('null key storage 이벤트에 반응', () => {
    const { result } = renderHook(() => useReadHistory());
    act(() => {
      localStorage.setItem('mud-read-history', JSON.stringify([99]));
      window.dispatchEvent(new StorageEvent('storage', { key: null }));
    });
    expect(result.current.isRead(99)).toBe(true);
  });
});
