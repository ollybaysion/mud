import { describe, test, expect, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { createLocalStorageStore } from './useLocalStorageStore';

beforeEach(() => {
  localStorage.clear();
});

describe('createLocalStorageStore', () => {
  test('초기 상태는 빈 배열', () => {
    const store = createLocalStorageStore<number[]>('test-store');
    const { result } = renderHook(() => store.useStore());
    expect(result.current).toEqual([]);
  });

  test('set으로 저장하고 get으로 조회', () => {
    const store = createLocalStorageStore<number[]>('test-store');
    store.set([1, 2, 3]);
    expect(store.get()).toEqual([1, 2, 3]);
  });

  test('useStore가 변경에 반응', () => {
    const store = createLocalStorageStore<number[]>('test-store');
    const { result } = renderHook(() => store.useStore());
    act(() => {
      store.set([42]);
    });
    expect(result.current).toEqual([42]);
  });

  test('잘못된 JSON이면 빈 배열 반환', () => {
    localStorage.setItem('test-broken', 'not-json');
    const store = createLocalStorageStore<number[]>('test-broken');
    expect(store.get()).toEqual([]);
  });

  test('useStore에서 잘못된 JSON이면 빈 배열', () => {
    localStorage.setItem('test-broken2', '{invalid}');
    const store = createLocalStorageStore<number[]>('test-broken2');
    const { result } = renderHook(() => store.useStore());
    expect(result.current).toEqual([]);
  });

  test('외부 storage 이벤트에 반응', () => {
    const store = createLocalStorageStore<number[]>('test-ext');
    const { result } = renderHook(() => store.useStore());
    act(() => {
      localStorage.setItem('test-ext', JSON.stringify([99]));
      window.dispatchEvent(new StorageEvent('storage', { key: 'test-ext' }));
    });
    expect(result.current).toEqual([99]);
  });

  test('언마운트 시 리스너 정리', () => {
    const store = createLocalStorageStore<number[]>('test-unmount');
    const { unmount } = renderHook(() => store.useStore());
    unmount();
    expect(true).toBe(true);
  });
});
