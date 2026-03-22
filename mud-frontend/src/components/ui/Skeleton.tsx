export function CardSkeleton() {
  return (
    <div className="trend-card skeleton-card">
      <div className="trend-card-header">
        <span className="skeleton-line" style={{ width: '80px', height: '20px' }} />
        <span className="skeleton-line" style={{ width: '50px', height: '20px' }} />
      </div>
      <div className="skeleton-line" style={{ width: '90%', height: '16px' }} />
      <div className="skeleton-line" style={{ width: '70%', height: '16px' }} />
      <div className="skeleton-line" style={{ width: '100%', height: '40px' }} />
      <div className="trend-card-footer">
        <span className="skeleton-line" style={{ width: '60px', height: '14px' }} />
        <span className="skeleton-line" style={{ width: '40px', height: '14px', marginLeft: 'auto' }} />
      </div>
    </div>
  );
}

export function GridSkeleton({ count = 6 }: { count?: number }) {
  return (
    <div className="trend-grid">
      {Array.from({ length: count }, (_, i) => (
        <CardSkeleton key={i} />
      ))}
    </div>
  );
}

export function FilterBarSkeleton() {
  return (
    <div className="filter-bar">
      <div className="skeleton-line" style={{ width: '240px', height: '36px', borderRadius: '6px' }} />
      <div className="skeleton-line" style={{ width: '160px', height: '36px', borderRadius: '6px' }} />
    </div>
  );
}
