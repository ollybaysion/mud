'use client';

import { useState, useEffect } from 'react';

const STORAGE_KEY = 'mud-visited';

export function OnboardingGuide() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (!localStorage.getItem(STORAGE_KEY)) {
      setVisible(true);
    }
  }, []);

  const handleDismiss = () => {
    localStorage.setItem(STORAGE_KEY, 'true');
    setVisible(false);
  };

  if (!visible) return null;

  return (
    <div style={{
      background: 'var(--color-surface)',
      border: '1px solid var(--color-border)',
      borderRadius: '8px',
      padding: '24px',
      marginBottom: '24px',
      textAlign: 'center',
    }}>
      <div style={{ fontSize: '28px', marginBottom: '8px' }}>⚗️</div>
      <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px' }}>
        Mud에 오신 것을 환영합니다!
      </h2>
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '8px',
        textAlign: 'left',
        maxWidth: '360px',
        margin: '0 auto 20px',
        fontSize: '13px',
        color: 'var(--color-text-muted)',
      }}>
        <div>📊 <strong style={{ color: 'var(--color-text)' }}>매일</strong> — 주요 트렌드에서 오늘의 핵심 확인</div>
        <div>📰 <strong style={{ color: 'var(--color-text)' }}>매주</strong> — 주간 다이제스트로 한 주를 한눈에</div>
        <div>🔬 <strong style={{ color: 'var(--color-text)' }}>필요할 때</strong> — AI 심층 분석으로 깊이 파고들기</div>
        <div>📌 <strong style={{ color: 'var(--color-text)' }}>관심 있으면</strong> — 북마크로 저장</div>
      </div>
      <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
        <button
          type="button"
          onClick={handleDismiss}
          style={{
            padding: '8px 20px',
            background: 'var(--color-accent)',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            fontWeight: 500,
            fontSize: '13px',
            cursor: 'pointer',
          }}
        >
          시작하기
        </button>
        <button
          type="button"
          onClick={handleDismiss}
          style={{
            padding: '8px 20px',
            background: 'none',
            color: 'var(--color-text-muted)',
            border: '1px solid var(--color-border)',
            borderRadius: '6px',
            fontSize: '13px',
            cursor: 'pointer',
          }}
        >
          다시 보지 않기
        </button>
      </div>
    </div>
  );
}
