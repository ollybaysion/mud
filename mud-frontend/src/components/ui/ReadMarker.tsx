'use client';

import { useEffect } from 'react';
import { useReadHistory } from '@/lib/useReadHistory';

export function ReadMarker({ itemId }: { itemId: number }) {
  const { markAsRead } = useReadHistory();

  useEffect(() => {
    markAsRead(itemId);
  }, [itemId, markAsRead]);

  return null;
}
