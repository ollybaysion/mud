'use client';

import { useSyncExternalStore } from 'react';

export function createLocalStorageStore<T>(key: string) {
  let listeners: Array<() => void> = [];
  let cachedSnapshot = '[]';
  let storageListenerAttached = false;

  function getRaw(): string {
    if (typeof window === 'undefined') return '[]';
    return localStorage.getItem(key) || '[]';
  }

  function setRaw(value: string) {
    localStorage.setItem(key, value);
    window.dispatchEvent(new StorageEvent('storage', { key }));
  }

  function handleStorage(e: StorageEvent) {
    if (e.key === key || e.key === null) {
      cachedSnapshot = getRaw();
      listeners.forEach((l) => l());
    }
  }

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
    const next = getRaw();
    if (next !== cachedSnapshot) {
      cachedSnapshot = next;
    }
    return cachedSnapshot;
  }

  function getServerSnapshot(): string {
    return '[]';
  }

  function get(): T {
    try {
      return JSON.parse(getRaw());
    } catch {
      return [] as unknown as T;
    }
  }

  function set(value: T) {
    setRaw(JSON.stringify(value));
  }

  function useStore(): T {
    const raw = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
    try {
      return JSON.parse(raw);
    } catch {
      return [] as unknown as T;
    }
  }

  return { get, set, useStore };
}
