import type { Category, PageResponse, TrendFilter, TrendItem, TrendStats, WeeklyReport } from './types';

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

async function apiFetch<T>(
  path: string,
  params?: Record<string, string | number | undefined>,
  revalidate = 60
): Promise<T> {
  const url = new URL(`${API_BASE}${path}`);
  if (params) {
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        url.searchParams.set(k, String(v));
      }
    });
  }

  const res = await fetch(url.toString(), {
    next: { revalidate },
  });

  if (!res.ok) {
    throw new Error(`API error ${res.status}: ${path}`);
  }

  return res.json();
}

export const api = {
  getTrends: (filter: TrendFilter = {}) =>
    apiFetch<PageResponse<TrendItem>>('/api/trends', {
      page: filter.page,
      size: filter.size ?? 20,
      category: filter.category,
      source: filter.source,
      minScore: filter.minScore,
      keyword: filter.keyword,
    }),

  getTrend: (id: number) =>
    apiFetch<TrendItem>(`/api/trends/${id}`, undefined, 300),

  getCategories: () =>
    apiFetch<Category[]>('/api/categories', undefined, 3600),

  getStats: () =>
    apiFetch<TrendStats>('/api/stats', undefined, 300),

  getWeeklyReport: (week?: string) =>
    apiFetch<WeeklyReport>('/api/reports/weekly', week ? { week } : undefined, 3600),

};
