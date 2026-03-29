'use client';

import { useCallback, useSyncExternalStore } from 'react';
import type { TrendItem } from './types';

const STORAGE_KEY = 'mud-bookmarks';

function getBookmarks(): TrendItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function setBookmarks(items: TrendItem[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  // Notify same-tab listeners
  window.dispatchEvent(new StorageEvent('storage', { key: STORAGE_KEY }));
}

let listeners: Array<() => void> = [];
let cachedSnapshot = '[]';

function getBookmarksSnapshot(): string {
  if (typeof window === 'undefined') return '[]';
  return localStorage.getItem(STORAGE_KEY) || '[]';
}

function handleStorage(e: StorageEvent) {
  if (e.key === STORAGE_KEY || e.key === null) {
    cachedSnapshot = getBookmarksSnapshot();
    listeners.forEach((l) => l());
  }
}

let storageListenerAttached = false;

function subscribe(listener: () => void): () => void {
  listeners = [...listeners, listener];
  if (!storageListenerAttached) {
    window.addEventListener('storage', handleStorage);
    storageListenerAttached = true;
  }
  return () => {
    listeners = listeners.filter((l) => l !== listener);
    if (listeners.length === 0) {
      window.removeEventListener('storage', handleStorage);
      storageListenerAttached = false;
    }
  };
}

function getSnapshot(): string {
  const next = getBookmarksSnapshot();
  if (next !== cachedSnapshot) {
    cachedSnapshot = next;
  }
  return cachedSnapshot;
}

function getServerSnapshot(): string {
  return '[]';
}

export function useBookmarks() {
  const raw = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
  let bookmarks: TrendItem[];
  try {
    bookmarks = JSON.parse(raw);
  } catch {
    bookmarks = [];
  }

  const isBookmarked = useCallback(
    (id: number) => bookmarks.some((item) => item.id === id),
    [bookmarks],
  );

  const toggleBookmark = useCallback((item: TrendItem) => {
    const current = getBookmarks();
    const exists = current.some((b) => b.id === item.id);
    if (exists) {
      setBookmarks(current.filter((b) => b.id !== item.id));
    } else {
      setBookmarks([item, ...current]);
    }
  }, []);

  return { bookmarks, isBookmarked, toggleBookmark };
}
