import { GridSkeleton, FilterBarSkeleton } from '@/components/ui/Skeleton';

export default function TrendsLoading() {
  return (
    <>
      <div style={{ marginBottom: '20px' }}>
        <div className="skeleton-line" style={{ width: '200px', height: '24px', marginBottom: '8px' }} />
        <div className="skeleton-line" style={{ width: '160px', height: '14px' }} />
      </div>
      <FilterBarSkeleton />
      <GridSkeleton count={6} />
    </>
  );
}
