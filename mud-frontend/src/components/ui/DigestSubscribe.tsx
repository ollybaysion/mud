'use client';

import { useState } from 'react';

export function DigestSubscribe() {
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'already' | 'error'>('idle');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;

    setStatus('loading');
    try {
      const res = await fetch('/api/digest/subscribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim() }),
      });

      if (res.ok) {
        setStatus('success');
        setEmail('');
      } else if (res.status === 409) {
        setStatus('already');
      } else {
        setStatus('error');
      }
    } catch {
      setStatus('error');
    }

    setTimeout(() => setStatus('idle'), 4000);
  };

  return (
    <div style={{
      padding: '16px 20px',
      borderTop: '1px solid var(--color-border)',
      marginTop: '12px',
    }}>
      <div style={{ fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
        📧 데일리 다이제스트
      </div>
      <p style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginBottom: '8px' }}>
        매일 아침 상위 트렌드를 이메일로 받아보세요
      </p>
      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '4px' }}>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="email@example.com"
          required
          disabled={status === 'loading'}
          style={{
            flex: 1,
            padding: '6px 8px',
            fontSize: '12px',
            background: 'var(--color-bg)',
            border: '1px solid var(--color-border)',
            borderRadius: '4px',
            color: 'var(--color-text)',
            outline: 'none',
            minWidth: 0,
          }}
        />
        <button
          type="submit"
          disabled={status === 'loading'}
          style={{
            padding: '6px 10px',
            fontSize: '11px',
            background: 'var(--color-accent)',
            color: '#fff',
            border: 'none',
            borderRadius: '4px',
            cursor: status === 'loading' ? 'not-allowed' : 'pointer',
            whiteSpace: 'nowrap',
            opacity: status === 'loading' ? 0.6 : 1,
          }}
        >
          {status === 'loading' ? '...' : '구독'}
        </button>
      </form>
      {status === 'success' && (
        <p style={{ fontSize: '11px', color: '#10b981', marginTop: '6px' }}>
          인증 이메일을 보냈습니다. 이메일을 확인해주세요.
        </p>
      )}
      {status === 'already' && (
        <p style={{ fontSize: '11px', color: '#f59e0b', marginTop: '6px' }}>
          이미 구독 중입니다.
        </p>
      )}
      {status === 'error' && (
        <p style={{ fontSize: '11px', color: '#dc2626', marginTop: '6px' }}>
          구독 신청에 실패했습니다.
        </p>
      )}
    </div>
  );
}
