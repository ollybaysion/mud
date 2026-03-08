'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { useState, useTransition } from 'react';

const SOURCES = [
  { value: '', label: '모든 소스' },
  { value: 'GITHUB', label: '🐙 GitHub' },
  { value: 'HACKER_NEWS', label: '🧡 Hacker News' },
  { value: 'DEV_TO', label: '💻 dev.to' },
  { value: 'ARXIV', label: '📄 ArXiv' },
  { value: 'REDDIT', label: '🔴 Reddit' },
];

interface Props {
  currentSource?: string;
  currentKeyword?: string;
}

export function FilterBar({ currentSource, currentKeyword }: Props) {
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
    <div className="filter-bar">
      <form onSubmit={handleKeywordSubmit} style={{ display: 'flex', gap: '8px' }}>
        <input
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

      <select
        className="source-select"
        value={currentSource ?? ''}
        onChange={(e) => updateParam('source', e.target.value)}
      >
        {SOURCES.map((s) => (
          <option key={s.value} value={s.value}>{s.label}</option>
        ))}
      </select>
    </div>
  );
}
