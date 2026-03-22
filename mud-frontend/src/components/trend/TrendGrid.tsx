import type { TrendItem } from '@/lib/types';
import { TrendCard } from './TrendCard';

interface Props {
  items: TrendItem[];
}

export function TrendGrid({ items }: Props) {
  if (items.length === 0) {
    return (
      <div className="empty-state">
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔍</div>
        <p>조건에 맞는 트렌드가 없습니다.</p>
        <p style={{ fontSize: '12px', marginTop: '8px' }}>필터를 변경하거나 다른 카테고리를 선택해보세요.</p>
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
