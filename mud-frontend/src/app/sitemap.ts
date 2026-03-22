import type { MetadataRoute } from 'next';
import { api } from '@/lib/api';

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://mud-frontend-production.up.railway.app';

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const entries: MetadataRoute.Sitemap = [
    {
      url: `${SITE_URL}/trends`,
      lastModified: new Date(),
      changeFrequency: 'hourly',
      priority: 1,
    },
  ];

  try {
    const data = await api.getTrends({ page: 0, size: 50, minScore: 3 });
    for (const item of data.content) {
      entries.push({
        url: `${SITE_URL}/trends/${item.id}`,
        lastModified: new Date(item.publishedAt ?? item.crawledAt),
        changeFrequency: 'weekly',
        priority: 0.7,
      });
    }
  } catch {
    // API 실패 시 메인 페이지만 포함
  }

  return entries;
}
