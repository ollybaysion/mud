import { describe, test, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MobileNav } from './MobileNav';

describe('MobileNav', () => {
  test('메뉴 버튼 표시', () => {
    render(<MobileNav><div>sidebar content</div></MobileNav>);
    expect(screen.getByLabelText('메뉴 열기')).toBeTruthy();
  });

  test('메뉴 열기 클릭 시 사이드바 표시', () => {
    render(<MobileNav><div>sidebar content</div></MobileNav>);
    fireEvent.click(screen.getByLabelText('메뉴 열기'));
    expect(screen.getByText('sidebar content')).toBeTruthy();
    expect(screen.getByLabelText('메뉴 닫기')).toBeTruthy();
  });

  test('닫기 클릭 시 사이드바 숨김', () => {
    render(<MobileNav><div>sidebar content</div></MobileNav>);
    fireEvent.click(screen.getByLabelText('메뉴 열기'));
    fireEvent.click(screen.getByLabelText('메뉴 닫기'));
    expect(screen.queryByText('sidebar content')).toBeNull();
  });
});
