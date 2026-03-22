export function stripHtml(html: string): string {
  return html.replace(/<[^>]*>/g, '');
}

export function sanitizeUrl(url: string): string {
  try {
    const parsed = new URL(url);
    if (parsed.protocol === 'http:' || parsed.protocol === 'https:') {
      return url;
    }
  } catch {
    // invalid URL
  }
  return '#';
}
