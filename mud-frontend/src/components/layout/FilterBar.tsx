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
  { value: 'PAPERS_WITH_CODE', label: '🔬 Papers With Code' },
  { value: 'INFOQ', label: '📰 InfoQ' },
  { value: 'HUGGING_FACE', label: '🤗 Hugging Face' },
  { value: 'LOBSTERS', label: '🦞 Lobsters' },
  { value: 'INSIDE_JAVA', label: '☕ Inside Java' },
  { value: 'ISOCPP', label: '⚡ isocpp.org' },
  { value: 'TLDR_AI', label: '📧 TLDR' },
  { value: 'THE_NEW_STACK', label: '☁️ The New Stack' },
  { value: 'CNCF', label: '🐳 CNCF' },
  { value: 'STACKOVERFLOW_BLOG', label: '📚 Stack Overflow' },
  { value: 'MARTIN_FOWLER', label: '🏗️ Martin Fowler' },
  { value: 'JETBRAINS', label: '🧠 JetBrains' },
  { value: 'GEEKNEWS', label: '🇰🇷 GeekNews' },
  { value: 'NVIDIA_BLOG', label: '🟢 NVIDIA Blog' },
  { value: 'SERVE_THE_HOME', label: '🖥️ ServeTheHome' },
  { value: 'TOMS_HARDWARE', label: "🔧 Tom's Hardware" },
  { value: 'PHORONIX', label: '🐧 Phoronix' },
  { value: 'TECHPOWERUP', label: '⚡ TechPowerUp' },
  { value: 'HACKADAY', label: '🛠️ Hackaday' },
  { value: 'EE_TIMES', label: '📡 EE Times' },
  { value: 'SEMI_ENGINEERING', label: '🔬 Semi Engineering' },
  { value: 'CHIPS_AND_CHEESE', label: '🧀 Chips and Cheese' },
  { value: 'VIDEOCARDZ', label: '🎮 VideoCardz' },
  { value: 'CNX_SOFTWARE', label: '💾 CNX Software' },
];

const SCORE_OPTIONS = [
  { value: '1', label: '전체 (★1+)' },
  { value: '2', label: '참고 이상 (★2+)' },
  { value: '3', label: '유용 이상 (★3+)' },
  { value: '4', label: '중요 이상 (★4+)' },
  { value: '5', label: '즉시 적용 (★5)' },
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

      <select
        className="source-select"
        value={currentMinScore ?? '2'}
        onChange={(e) => updateParam('minScore', e.target.value)}
      >
        {SCORE_OPTIONS.map((s) => (
          <option key={s.value} value={s.value}>{s.label}</option>
        ))}
      </select>
    </div>
  );
}
