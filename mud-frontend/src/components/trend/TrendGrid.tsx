import type { TrendItem } from '@/lib/types';
import { TrendCard } from './TrendCard';

interface Props {
  items: TrendItem[];
}

export function TrendGrid({ items }: Props) {
  if (items.length === 0) {
    return (
      <div className="empty-state">
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌊</div>
        <p>트렌드 데이터가 없습니다.</p>
        <p style={{ fontSize: '12px', marginTop: '8px' }}>크롤러가 데이터를 수집하는 중입니다.</p>
      </div>
    );
  }

  return (
    <div className="trend-grid">
      {items.map((item) => (
        <TrendCard key={item.id} item={item} />
      ))}
    </div>
  );
}
