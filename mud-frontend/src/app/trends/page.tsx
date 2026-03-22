import type { Metadata } from 'next';
import { Suspense } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';
import { TrendGrid } from '@/components/trend/TrendGrid';
import { Pagination } from '@/components/ui/Pagination';
import { FilterBar } from '@/components/layout/FilterBar';
import { FilterBarSkeleton } from '@/components/ui/Skeleton';
import { relativeTime } from '@/lib/time';

export const metadata: Metadata = {
  title: '기술 트렌드',
  description: '18개 소스에서 수집한 최신 기술 트렌드를 AI가 분석하여 제공합니다.',
  openGraph: {
    title: 'Mud - 기술 트렌드 큐레이션',
    description: '현업 개발자를 위한 최신 기술 트렌드 큐레이션 플랫폼',
    type: 'website',
    siteName: 'Mud',
  },
};

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

  const isFirstPage = page === 0 && !params.category && !params.source && !params.keyword;

  const [trendsData, stats, topTrends] = await Promise.all([
    api.getTrends({
      page,
      size: 20,
      category: params.category,
      source: params.source,
      minScore: Number(params.minScore ?? 2),
      keyword: params.keyword,
    }).catch(() => ({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })),
    api.getStats().catch(() => null),
    isFirstPage
      ? api.getTrends({ page: 0, size: 5, minScore: 4 }).catch(() => ({ content: [] }))
      : Promise.resolve({ content: [] }),
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

      {isFirstPage && topTrends.content.length > 0 && (
        <div style={{
          marginBottom: '24px',
          padding: '16px',
          background: 'var(--color-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: '8px',
        }}>
          <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '12px', color: 'var(--color-text-muted)' }}>
            🔥 주요 트렌드
          </div>
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {topTrends.content.map((item) => (
              <li key={item.id} style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px' }}>
                <span style={{ color: '#a855f7', fontWeight: 600, flexShrink: 0 }}>★{item.relevanceScore}</span>
                <Link href={`/trends/${item.id}`} style={{ color: 'var(--color-text)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {item.title}
                </Link>
                <span style={{ fontSize: '11px', color: 'var(--color-text-muted)', flexShrink: 0 }}>
                  {relativeTime(item.publishedAt ?? item.crawledAt)}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      <Suspense fallback={<FilterBarSkeleton />}>
        <FilterBar
          currentSource={params.source}
          currentKeyword={params.keyword}
          currentMinScore={params.minScore}
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
