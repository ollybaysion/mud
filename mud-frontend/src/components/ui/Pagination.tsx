'use client';

import { useRouter, useSearchParams } from 'next/navigation';

interface Props {
  currentPage: number;
  totalPages: number;
}

export function Pagination({ currentPage, totalPages }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();

  if (totalPages <= 1) return null;

  const goToPage = (page: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set('page', String(page));
    router.push(`?${params.toString()}`);
  };

  const pages = Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
    if (totalPages <= 7) return i;
    if (currentPage < 4) return i;
    if (currentPage > totalPages - 4) return totalPages - 7 + i;
    return currentPage - 3 + i;
  });

  return (
    <nav className="pagination">
      <button
        className="page-btn"
        disabled={currentPage === 0}
        onClick={() => goToPage(currentPage - 1)}
      >
        ← 이전
      </button>

      {pages.map((page) => (
        <button
          key={page}
          className={`page-btn ${page === currentPage ? 'active' : ''}`}
          onClick={() => goToPage(page)}
        >
          {page + 1}
        </button>
      ))}

      <button
        className="page-btn"
        disabled={currentPage === totalPages - 1}
        onClick={() => goToPage(currentPage + 1)}
      >
        다음 →
      </button>
    </nav>
  );
}
