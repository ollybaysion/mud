'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { useState, useTransition } from 'react';
import { SOURCE_CONFIG } from '@/constants/sources';

const SOURCES = [
  { value: '', label: '모든 소스' },
  ...Object.entries(SOURCE_CONFIG).map(([key, conf]) => ({
    value: key,
    label: `${conf.emoji} ${conf.label}`,
  })),
];

const SCORE_OPTIONS = [
  { value: '1', label: '전체' },
  { value: '25', label: '★25+ 참고' },
  { value: '45', label: '★45+ 일반' },
  { value: '65', label: '★65+ 주요' },
  { value: '85', label: '★85+ 핵심' },
];

interface Props {
  currentSource?: string;
  currentKeyword?: string;
  currentMinScore?: string;
}

export function FilterBar({ currentSource, currentKeyword, currentMinScore }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [keyword, setKeyword] = useState(currentKeyword ?? '');
  const [, startTransition] = useTransition();

  const updateParam = (key: string, value: string) => {
    const params = new URLSearchParams(searchParams.toString());
    if (value) {
      params.set(key, value);
    } else {
      params.delete(key);
    }
    params.delete('page'); // Reset to page 0 on filter change
    startTransition(() => {
      router.push(`?${params.toString()}`);
    });
  };

  const handleKeywordSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateParam('keyword', keyword);
  };

  return (
    <div className="filter-bar" role="search" aria-label="트렌드 필터">
      <form onSubmit={handleKeywordSubmit} style={{ display: 'flex', gap: '8px' }}>
        <label htmlFor="keyword-search" className="sr-only">키워드 검색</label>
        <input
          id="keyword-search"
          type="search"
          className="search-input"
          placeholder="키워드 검색..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <button
          type="submit"
          className="page-btn"
          style={{ whiteSpace: 'nowrap' }}
        >
          검색
        </button>
      </form>

      <label htmlFor="source-filter" className="sr-only">소스 필터</label>
      <select
        id="source-filter"
        className="source-select"
        value={currentSource ?? ''}
        onChange={(e) => updateParam('source', e.target.value)}
      >
        {SOURCES.map((s) => (
          <option key={s.value} value={s.value}>{s.label}</option>
        ))}
      </select>

      <label htmlFor="score-filter" className="sr-only">점수 필터</label>
      <select
        id="score-filter"
        className="source-select"
        value={currentMinScore ?? '25'}
        onChange={(e) => updateParam('minScore', e.target.value)}
      >
        {SCORE_OPTIONS.map((s) => (
          <option key={s.value} value={s.value}>{s.label}</option>
        ))}
      </select>
    </div>
  );
}
