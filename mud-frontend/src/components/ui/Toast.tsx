'use client';

import { useEffect, useState } from 'react';

interface Props {
  message: string;
  visible: boolean;
}

export function Toast({ message, visible }: Props) {
  const [show, setShow] = useState(false);

  useEffect(() => {
    if (visible) {
      setShow(true);
      const timer = setTimeout(() => setShow(false), 2000);
      return () => clearTimeout(timer);
    }
  }, [visible, message]);

  if (!show) return null;

  return (
    <div style={{
      position: 'fixed',
      bottom: '24px',
      left: '50%',
      transform: 'translateX(-50%)',
      background: 'var(--color-surface)',
      border: '1px solid var(--color-border)',
      color: 'var(--color-text)',
      padding: '10px 20px',
      borderRadius: '8px',
      fontSize: '13px',
      zIndex: 1000,
      boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)',
    }}>
      {message}
    </div>
  );
}
