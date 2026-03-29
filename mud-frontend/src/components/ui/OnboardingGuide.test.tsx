import { describe, test, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { OnboardingGuide } from './OnboardingGuide';

describe('OnboardingGuide', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test('첫 방문 시 표시', () => {
    render(<OnboardingGuide />);
    expect(screen.getByText('Mud에 오신 것을 환영합니다!')).toBeTruthy();
  });

  test('이미 방문한 경우 숨김', () => {
    localStorage.setItem('mud-visited', 'true');
    const { container } = render(<OnboardingGuide />);
    expect(container.textContent).not.toContain('환영합니다');
  });

  test('시작하기 클릭 시 localStorage 설정됨', () => {
    render(<OnboardingGuide />);
    const buttons = screen.getAllByRole('button');
    fireEvent.click(buttons[0]); // 시작하기
    expect(localStorage.getItem('mud-visited')).toBe('true');
  });

  test('다시 보지 않기 클릭 시 localStorage 설정됨', () => {
    render(<OnboardingGuide />);
    const buttons = screen.getAllByRole('button');
    fireEvent.click(buttons[1]); // 다시 보지 않기
    expect(localStorage.getItem('mud-visited')).toBe('true');
  });
});
