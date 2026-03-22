import { Suspense } from 'react';
import { api } from '@/lib/api';
import { TrendGrid } from '@/components/trend/TrendGrid';
import { Pagination } from '@/components/ui/Pagination';
import { FilterBar } from '@/components/layout/FilterBar';

interface Props {
  searchParams: Promise<{
    category?: string;
    source?: string;
    page?: string;
    minScore?: string;
    keyword?: string;
  }>;
}

export default async function TrendsPage({ searchParams }: Props) {
  const params = await searchParams;
  const page = Number(params.page ?? 0);

  const [trendsData, stats] = await Promise.all([
    api.getTrends({
      page,
      size: 20,
      category: params.category,
      source: params.source,
      minScore: Number(params.minScore ?? 2),
      keyword: params.keyword,
    }).catch(() => ({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })),
    api.getStats().catch(() => null),
  ]);

  const categoryLabel = params.category
    ? trendsData.content[0]?.category?.displayName ?? params.category
    : '전체';

  return (
    <>
      <div style={{ marginBottom: '20px' }}>
        <h1 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '4px' }}>
          {categoryLabel === '전체' ? '오늘의 기술 트렌드' : `${categoryLabel} 트렌드`}
        </h1>
        {stats && (
          <p style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>
            {stats.totalItems.toLocaleString()}개의 기술 소식을 AI가 분석했습니다
          </p>
        )}
      </div>

      <Suspense fallback={null}>
        <FilterBar
          currentSource={params.source}
          currentKeyword={params.keyword}
        />
      </Suspense>

      <TrendGrid items={trendsData.content} />

      <Suspense fallback={null}>
        <Pagination
          currentPage={trendsData.number}
          totalPages={trendsData.totalPages}
        />
      </Suspense>
    </>
  );
}
