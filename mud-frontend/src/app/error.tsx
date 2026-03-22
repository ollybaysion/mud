'use client';

export default function Error({ reset }: { reset: () => void }) {
  return (
    <div style={{ textAlign: 'center', padding: '80px 20px' }}>
      <div style={{ fontSize: '64px', marginBottom: '16px' }}>⚠️</div>
      <h1 style={{ fontSize: '24px', fontWeight: 700, marginBottom: '8px' }}>
        분석 중 문제가 발생했습니다
      </h1>
      <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', marginBottom: '24px' }}>
        잠시 후 다시 시도해주세요.
      </p>
      <button
        type="button"
        onClick={reset}
        style={{
          padding: '10px 20px',
          background: 'var(--color-accent)',
          color: '#fff',
          border: 'none',
          borderRadius: '6px',
          fontWeight: 500,
          cursor: 'pointer',
        }}
      >
        다시 시도
      </button>
    </div>
  );
}
