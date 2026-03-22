'use client';

import { useState } from 'react';

interface Props {
  children: React.ReactNode;
}

export function MobileNav({ children }: Props) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button
        type="button"
        className="mobile-menu-btn"
        onClick={() => setOpen(true)}
        aria-label="메뉴 열기"
      >
        ☰
      </button>

      {open && (
        <div className="mobile-overlay" onClick={() => setOpen(false)} role="dialog" aria-modal="true" aria-label="네비게이션 메뉴">
          <div className="mobile-sidebar" onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              className="mobile-close-btn"
              onClick={() => setOpen(false)}
              aria-label="메뉴 닫기"
            >
              ✕
            </button>
            <div onClick={() => setOpen(false)}>
              {children}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
