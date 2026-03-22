'use server';

const API_BASE = process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export async function requestDeepAnalysis(id: number): Promise<{ deepAnalysis: string | null; error?: string }> {
  try {
    console.log(`[deep-analysis] Requesting: ${API_BASE}/api/trends/${id}/deep-analysis`);
    const res = await fetch(`${API_BASE}/api/trends/${id}/deep-analysis`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    });

    if (!res.ok) {
      const body = await res.text();
      console.error(`[deep-analysis] Backend error ${res.status}: ${body}`);
      return { deepAnalysis: null, error: `분석 요청 실패 (${res.status})` };
    }

    const data = await res.json();
    return { deepAnalysis: data.deepAnalysis };
  } catch (e) {
    console.error(`[deep-analysis] Fetch failed:`, e);
    return { deepAnalysis: null, error: e instanceof Error ? e.message : '서버 연결 실패' };
  }
}
