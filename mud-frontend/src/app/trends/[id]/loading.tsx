export default function DetailLoading() {
  return (
    <div style={{ maxWidth: '800px' }}>
      <div className="skeleton-line" style={{ width: '80px', height: '14px', marginBottom: '20px' }} />
      <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
        <div className="skeleton-line" style={{ width: '100px', height: '22px', borderRadius: '99px' }} />
        <div className="skeleton-line" style={{ width: '80px', height: '22px', borderRadius: '99px' }} />
      </div>
      <div className="skeleton-line" style={{ width: '90%', height: '24px', marginBottom: '12px' }} />
      <div className="skeleton-line" style={{ width: '60%', height: '14px', marginBottom: '24px' }} />
      <div style={{ background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: '6px', padding: '16px', marginBottom: '24px' }}>
        <div className="skeleton-line" style={{ width: '100%', height: '14px', marginBottom: '8px' }} />
        <div className="skeleton-line" style={{ width: '85%', height: '14px', marginBottom: '8px' }} />
        <div className="skeleton-line" style={{ width: '70%', height: '14px' }} />
      </div>
    </div>
  );
}
