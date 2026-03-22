import type { MetadataRoute } from 'next';

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://mud-frontend-production.up.railway.app';

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: '*',
      allow: '/',
      disallow: ['/api/', '/bookmarks'],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
