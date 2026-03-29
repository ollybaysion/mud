import type { Metadata } from 'next';
import { headers } from 'next/headers';
import './globals.css';
import { Sidebar } from '@/components/layout/Sidebar';
import { MobileNav } from '@/components/layout/MobileNav';
import { api } from '@/lib/api';

export const metadata: Metadata = {
  title: {
    default: 'Mud - 기술 트렌드',
    template: '%s | Mud',
  },
  description: '현업 개발자를 위한 최신 기술 트렌드 큐레이션',
  icons: {
    icon: '/favicon.svg',
    apple: '/favicon.svg',
  },
  openGraph: {
    type: 'website',
    siteName: 'Mud',
    title: 'Mud - 기술 트렌드 큐레이션',
    description: '18개 소스에서 수집한 최신 기술 트렌드를 AI가 분석하여 제공합니다.',
  },
  twitter: {
    card: 'summary',
  },
  robots: {
    index: true,
    follow: true,
  },
};

export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const headersList = await headers();
  const nonce = headersList.get('x-nonce') ?? '';
  const categories = await api.getCategories().catch(() => []);

  return (
    <html lang="ko">
      <head>
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@300;400;500;600;700&display=swap"
          nonce={nonce}
        />
      </head>
      <body nonce={nonce}>
        <div className="app-layout">
          <div className="sidebar-desktop">
            <Sidebar categories={categories} />
          </div>
          <MobileNav>
            <Sidebar categories={categories} />
          </MobileNav>
          <main className="main-content">{children}</main>
        </div>
      </body>
    </html>
  );
}
