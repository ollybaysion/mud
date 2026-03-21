'use server';

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export async function requestDeepAnalysis(id: number): Promise<{ deepAnalysis: string | null; error?: string }> {
  const res = await fetch(`${API_BASE}/api/trends/${id}/deep-analysis`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });

  if (!res.ok) {
    return { deepAnalysis: null, error: `분석 요청 실패 (${res.status})` };
  }

  const data = await res.json();
  return { deepAnalysis: data.deepAnalysis };
}
