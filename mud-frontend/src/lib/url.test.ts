import { describe, test, expect } from 'vitest';
import { sanitizeUrl, stripHtml } from './url';

describe('sanitizeUrl', () => {
  test('https URL을 통과시킨다', () => {
    expect(sanitizeUrl('https://example.com')).toBe('https://example.com');
  });

  test('http URL을 통과시킨다', () => {
    expect(sanitizeUrl('http://example.com/path?q=1')).toBe('http://example.com/path?q=1');
  });

  test('javascript: 스킴을 차단한다', () => {
    expect(sanitizeUrl('javascript:alert(1)')).toBe('#');
  });

  test('data: 스킴을 차단한다', () => {
    expect(sanitizeUrl('data:text/html,<h1>XSS</h1>')).toBe('#');
  });

  test('빈 문자열은 #을 반환한다', () => {
    expect(sanitizeUrl('')).toBe('#');
  });

  test('잘못된 URL은 #을 반환한다', () => {
    expect(sanitizeUrl('not-a-url')).toBe('#');
  });
});

describe('stripHtml', () => {
  test('HTML 태그를 제거한다', () => {
    expect(stripHtml('<p>Hello <b>World</b></p>')).toBe('Hello World');
  });

  test('태그 없는 텍스트는 그대로 반환한다', () => {
    expect(stripHtml('plain text')).toBe('plain text');
  });

  test('빈 문자열은 빈 문자열을 반환한다', () => {
    expect(stripHtml('')).toBe('');
  });

  test('중첩 태그를 제거한다', () => {
    expect(stripHtml('<div><span>nested</span></div>')).toBe('nested');
  });

  test('self-closing 태그를 제거한다', () => {
    expect(stripHtml('before<br/>after')).toBe('beforeafter');
  });
});
