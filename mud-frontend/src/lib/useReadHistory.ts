'use client';

import { useCallback, useSyncExternalStore } from 'react';

const STORAGE_KEY = 'mud-read-history';
const MAX_ITEMS = 500;

function getReadIds(): number[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function setReadIds(ids: number[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(ids.slice(0, MAX_ITEMS)));
  window.dispatchEvent(new StorageEvent('storage', { key: STORAGE_KEY }));
}

let listeners: Array<() => void> = [];
let cachedSnapshot = '[]';

function getReadSnapshot(): string {
  if (typeof window === 'undefined') return '[]';
  return localStorage.getItem(STORAGE_KEY) || '[]';
}

function handleStorage(e: StorageEvent) {
  if (e.key === STORAGE_KEY || e.key === null) {
    cachedSnapshot = getReadSnapshot();
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
  const next = getReadSnapshot();
  if (next !== cachedSnapshot) {
    cachedSnapshot = next;
  }
  return cachedSnapshot;
}

function getServerSnapshot(): string {
  return '[]';
}

export function useReadHistory() {
  const raw = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
  let readIds: number[];
  try {
    readIds = JSON.parse(raw);
  } catch {
    readIds = [];
  }

  const isRead = useCallback(
    (id: number) => readIds.includes(id),
    [readIds],
  );

  const markAsRead = useCallback((id: number) => {
    const current = getReadIds();
    if (!current.includes(id)) {
      setReadIds([id, ...current]);
    }
  }, []);

  return { readIds, isRead, markAsRead };
}
