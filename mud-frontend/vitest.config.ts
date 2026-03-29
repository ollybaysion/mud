import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/__tests__/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'text-summary', 'html', 'json-summary'],
      include: ['src/lib/**', 'src/constants/**', 'src/components/**'],
      exclude: [
        'src/lib/api.ts',
        'src/lib/actions.ts',
        'src/lib/types.ts',
        'src/lib/useDeepAnalysis.ts',
        'src/components/ui/DeepAnalysisSection.tsx',
        'src/components/layout/Sidebar.tsx',
        'src/components/layout/FilterBar.tsx',
        'src/components/trend/TrendCard.tsx',
        'src/components/ui/Pagination.tsx',
      ],
      thresholds: {
        statements: 75,
        branches: 60,
        functions: 65,
        lines: 75,
      },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
});
