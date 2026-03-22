import Link from 'next/link';

export default function NotFound() {
  return (
    <div style={{ textAlign: 'center', padding: '80px 20px' }}>
      <div style={{ fontSize: '64px', marginBottom: '16px' }}>🔍</div>
      <h1 style={{ fontSize: '24px', fontWeight: 700, marginBottom: '8px' }}>
        이 트렌드는 아직 발견되지 않았습니다
      </h1>
      <p style={{ fontSize: '14px', color: 'var(--color-text-muted)', marginBottom: '24px' }}>
        요청하신 페이지를 찾을 수 없습니다.
      </p>
      <Link
        href="/trends"
        style={{
          display: 'inline-block',
          padding: '10px 20px',
          background: 'var(--color-accent)',
          color: '#fff',
          borderRadius: '6px',
          fontWeight: 500,
        }}
      >
        전체 트렌드 보기
      </Link>
    </div>
  );
}
