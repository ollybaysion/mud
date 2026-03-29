import { describe, test, expect } from 'vitest';
import { getScoreColor, getScoreLabel, SOURCE_CONFIG } from './sources';

describe('getScoreColor', () => {
  test('85+ 는 보라색', () => {
    expect(getScoreColor(85)).toBe('#a855f7');
    expect(getScoreColor(100)).toBe('#a855f7');
  });

  test('65~84는 초록색', () => {
    expect(getScoreColor(65)).toBe('#10b981');
    expect(getScoreColor(84)).toBe('#10b981');
  });

  test('45~64는 파란색', () => {
    expect(getScoreColor(45)).toBe('#3b82f6');
  });

  test('25~44는 노란색', () => {
    expect(getScoreColor(25)).toBe('#f59e0b');
  });

  test('25 미만은 회색', () => {
    expect(getScoreColor(0)).toBe('#64748b');
    expect(getScoreColor(24)).toBe('#64748b');
  });
});

describe('getScoreLabel', () => {
  test('85+ 는 핵심 트렌드', () => {
    expect(getScoreLabel(90)).toBe('핵심 트렌드');
  });

  test('65~84는 주요 트렌드', () => {
    expect(getScoreLabel(70)).toBe('주요 트렌드');
  });

  test('45~64는 알아두면 유용', () => {
    expect(getScoreLabel(50)).toBe('알아두면 유용');
  });

  test('25~44는 참고 수준', () => {
    expect(getScoreLabel(30)).toBe('참고 수준');
  });

  test('25 미만은 관련성 낮음', () => {
    expect(getScoreLabel(10)).toBe('관련성 낮음');
  });
});

describe('SOURCE_CONFIG', () => {
  test('GITHUB 설정이 존재한다', () => {
    expect(SOURCE_CONFIG.GITHUB).toBeDefined();
    expect(SOURCE_CONFIG.GITHUB.label).toBe('GitHub');
  });

  test('모든 소스에 label, color, emoji가 있다', () => {
    Object.entries(SOURCE_CONFIG).forEach(([key, config]) => {
      expect(config.label, `${key} label`).toBeTruthy();
      expect(config.color, `${key} color`).toMatch(/^#/);
      expect(config.emoji, `${key} emoji`).toBeTruthy();
    });
  });
});
