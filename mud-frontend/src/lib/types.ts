export type CrawlSource = 'GITHUB' | 'HACKER_NEWS' | 'DEV_TO' | 'ARXIV' | 'REDDIT'
  | 'PAPERS_WITH_CODE' | 'INFOQ' | 'HUGGING_FACE' | 'LOBSTERS'
  | 'INSIDE_JAVA' | 'ISOCPP' | 'TLDR_AI' | 'THE_NEW_STACK'
  | 'CNCF' | 'STACKOVERFLOW_BLOG' | 'MARTIN_FOWLER' | 'JETBRAINS'
  | 'GEEKNEWS';

export interface Category {
  id: number;
  slug: string;
  displayName: string;
  emoji: string;
  sortOrder: number;
}

export interface TrendItem {
  id: number;
  title: string;
  originalUrl: string;
  source: CrawlSource;
  description: string | null;
  koreanSummary: string | null;
  deepAnalysis: string | null;
  category: Category | null;
  relevanceScore: number | null;
  keywords: string[];
  publishedAt: string | null;
  crawledAt: string;
  githubStars: number | null;
  githubLanguage: string | null;
}

export interface TrendStats {
  totalItems: number;
  itemsBySource: Record<string, number>;
  itemsByCategory: Record<string, number>;
  generatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface TrendFilter {
  page?: number;
  size?: number;
  category?: string;
  source?: string;
  minScore?: number;
  keyword?: string;
}
