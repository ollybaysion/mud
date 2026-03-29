import { describe, test, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DigestSubscribe } from './DigestSubscribe';

describe('DigestSubscribe', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  test('구독 폼 표시', () => {
    render(<DigestSubscribe />);
    expect(screen.getByText('📧 데일리 다이제스트')).toBeTruthy();
    expect(screen.getByPlaceholderText('email@example.com')).toBeTruthy();
    expect(screen.getByText('구독')).toBeTruthy();
  });

  test('이메일 입력', () => {
    render(<DigestSubscribe />);
    const input = screen.getByPlaceholderText('email@example.com') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'test@example.com' } });
    expect(input.value).toBe('test@example.com');
  });

  test('구독 성공 시 메시지 표시', async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: true, status: 200 });
    render(<DigestSubscribe />);
    const input = screen.getByPlaceholderText('email@example.com');
    fireEvent.change(input, { target: { value: 'test@example.com' } });
    fireEvent.submit(input.closest('form')!);
    await vi.waitFor(() => {
      expect(screen.getByText('인증 이메일을 보냈습니다. 이메일을 확인해주세요.')).toBeTruthy();
    });
  });

  test('409 시 이미 구독 메시지', async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: false, status: 409 });
    render(<DigestSubscribe />);
    const input = screen.getByPlaceholderText('email@example.com');
    fireEvent.change(input, { target: { value: 'test@example.com' } });
    fireEvent.submit(input.closest('form')!);
    await vi.waitFor(() => {
      expect(screen.getByText('이미 구독 중입니다.')).toBeTruthy();
    });
  });

  test('에러 시 실패 메시지', async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: false, status: 500 });
    render(<DigestSubscribe />);
    const input = screen.getByPlaceholderText('email@example.com');
    fireEvent.change(input, { target: { value: 'test@example.com' } });
    fireEvent.submit(input.closest('form')!);
    await vi.waitFor(() => {
      expect(screen.getByText('구독 신청에 실패했습니다.')).toBeTruthy();
    });
  });
});
