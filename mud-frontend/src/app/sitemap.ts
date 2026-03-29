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
    {
      url: `${SITE_URL}/digest`,
      lastModified: new Date(),
      changeFrequency: 'weekly',
      priority: 0.9,
    },
  ];

  try {
    // 상위 200건 수집 (4 페이지 × 50건)
    for (let page = 0; page < 4; page++) {
      const data = await api.getTrends({ page, size: 50, minScore: 25 });
      for (const item of data.content) {
        entries.push({
          url: `${SITE_URL}/trends/${item.id}`,
          lastModified: new Date(item.publishedAt ?? item.crawledAt),
          changeFrequency: 'weekly',
          priority: 0.7,
        });
      }
      if (data.content.length < 50) break;
    }
  } catch {
    // API 실패 시 정적 페이지만 포함
  }

  // 카테고리 페이지
  try {
    const categories = await api.getCategories();
    for (const cat of categories) {
      entries.push({
        url: `${SITE_URL}/trends?category=${cat.slug}`,
        lastModified: new Date(),
        changeFrequency: 'daily',
        priority: 0.8,
      });
    }
  } catch {
    // 무시
  }

  return entries;
}
