import type { Metadata } from 'next';
import './globals.css';
import { Sidebar } from '@/components/layout/Sidebar';
import { api } from '@/lib/api';

export const metadata: Metadata = {
  title: 'Mud - 기술 트렌드',
  description: '현업 개발자를 위한 최신 기술 트렌드 큐레이션',
};

export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const categories = await api.getCategories().catch(() => []);

  return (
    <html lang="ko">
      <body>
        <div className="app-layout">
          <Sidebar categories={categories} />
          <main className="main-content">{children}</main>
        </div>
      </body>
    </html>
  );
}
