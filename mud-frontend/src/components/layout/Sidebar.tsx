import Link from 'next/link';
import type { Category } from '@/lib/types';

interface Props {
  categories: Category[];
}

const SOURCE_LINKS = [
  { href: '/trends?source=GITHUB', label: 'GitHub Trending', emoji: '🐙' },
  { href: '/trends?source=HACKER_NEWS', label: 'Hacker News', emoji: '🧡' },
  { href: '/trends?source=DEV_TO', label: 'dev.to', emoji: '💻' },
  { href: '/trends?source=ARXIV', label: 'ArXiv', emoji: '📄' },
  { href: '/trends?source=REDDIT', label: 'Reddit', emoji: '🔴' },
  { href: '/trends?source=NVIDIA_BLOG', label: 'NVIDIA Blog', emoji: '🟢' },
  { href: '/trends?source=SERVE_THE_HOME', label: 'ServeTheHome', emoji: '🖥️' },
  { href: '/trends?source=TOMS_HARDWARE', label: "Tom's Hardware", emoji: '🔧' },
  { href: '/trends?source=PHORONIX', label: 'Phoronix', emoji: '🐧' },
  { href: '/trends?source=TECHPOWERUP', label: 'TechPowerUp', emoji: '⚡' },
  { href: '/trends?source=HACKADAY', label: 'Hackaday', emoji: '🛠️' },
  { href: '/trends?source=EE_TIMES', label: 'EE Times', emoji: '📡' },
  { href: '/trends?source=SEMI_ENGINEERING', label: 'Semi Engineering', emoji: '🔬' },
  { href: '/trends?source=CHIPS_AND_CHEESE', label: 'Chips and Cheese', emoji: '🧀' },
  { href: '/trends?source=VIDEOCARDZ', label: 'VideoCardz', emoji: '🎮' },
  { href: '/trends?source=CNX_SOFTWARE', label: 'CNX Software', emoji: '💾' },
];

export function Sidebar({ categories }: Props) {
  return (
    <aside className="sidebar">
      <div className="header">
        <div className="header-logo">⚗️ Mud</div>
        <div className="header-tagline">기술 트렌드 큐레이션</div>
      </div>

      <nav>
        <ul className="category-list">
          <li className="category-item" style={{ marginBottom: '4px' }}>
            <Link href="/trends">
              <span>📊</span> 전체 보기
            </Link>
          </li>
          <li className="category-item">
            <Link href="/bookmarks">
              <span>📌</span> 저장한 글
            </Link>
          </li>
        </ul>

        <div style={{ padding: '12px 20px 6px', fontSize: '11px', color: 'var(--color-text-muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          카테고리
        </div>
        <ul className="category-list">
          {categories.map((cat) => (
            <li key={cat.slug} className="category-item">
              <Link href={`/trends?category=${cat.slug}`}>
                <span>{cat.emoji}</span>
                {cat.displayName}
              </Link>
            </li>
          ))}
        </ul>

        <div style={{ padding: '12px 20px 6px', fontSize: '11px', color: 'var(--color-text-muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          소스
        </div>
        <ul className="category-list">
          {SOURCE_LINKS.map((src) => (
            <li key={src.href} className="category-item">
              <Link href={src.href}>
                <span>{src.emoji}</span>
                {src.label}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  );
}
