export const SOURCE_CONFIG: Record<string, { label: string; color: string; emoji: string }> = {
  GITHUB: { label: 'GitHub', color: '#e2e8f0', emoji: '🐙' },
  HACKER_NEWS: { label: 'Hacker News', color: '#ff6600', emoji: '🧡' },
  DEV_TO: { label: 'dev.to', color: '#3b49df', emoji: '💻' },
  ARXIV: { label: 'ArXiv', color: '#b31b1b', emoji: '📄' },
  REDDIT: { label: 'Reddit', color: '#ff4500', emoji: '🔴' },
  PAPERS_WITH_CODE: { label: 'Papers With Code', color: '#21cbce', emoji: '🔬' },
  INFOQ: { label: 'InfoQ', color: '#e8372e', emoji: '📰' },
  HUGGING_FACE: { label: 'Hugging Face', color: '#ff9d00', emoji: '🤗' },
  LOBSTERS: { label: 'Lobsters', color: '#ac130d', emoji: '🦞' },
  INSIDE_JAVA: { label: 'Inside Java', color: '#f89820', emoji: '☕' },
  ISOCPP: { label: 'isocpp.org', color: '#00599c', emoji: '⚡' },
  TLDR_AI: { label: 'TLDR', color: '#1a73e8', emoji: '📧' },
  THE_NEW_STACK: { label: 'The New Stack', color: '#009bde', emoji: '☁️' },
  CNCF: { label: 'CNCF', color: '#446ca9', emoji: '🐳' },
  STACKOVERFLOW_BLOG: { label: 'Stack Overflow', color: '#f48024', emoji: '📚' },
  MARTIN_FOWLER: { label: 'Martin Fowler', color: '#5b2d8e', emoji: '🏗️' },
  JETBRAINS: { label: 'JetBrains', color: '#ff318c', emoji: '🧠' },
  GEEKNEWS: { label: 'GeekNews', color: '#00c4b3', emoji: '🇰🇷' },
  NVIDIA_BLOG: { label: 'NVIDIA Blog', color: '#76b900', emoji: '🟢' },
  SERVE_THE_HOME: { label: 'ServeTheHome', color: '#0071c5', emoji: '🖥️' },
  TOMS_HARDWARE: { label: "Tom's Hardware", color: '#e00034', emoji: '🔧' },
  PHORONIX: { label: 'Phoronix', color: '#6a8759', emoji: '🐧' },
  TECHPOWERUP: { label: 'TechPowerUp', color: '#0060aa', emoji: '⚡' },
  HACKADAY: { label: 'Hackaday', color: '#d4a017', emoji: '🛠️' },
  EE_TIMES: { label: 'EE Times', color: '#cc0000', emoji: '📡' },
  SEMI_ENGINEERING: { label: 'Semi Engineering', color: '#003366', emoji: '🔬' },
  CHIPS_AND_CHEESE: { label: 'Chips and Cheese', color: '#f5a623', emoji: '🧀' },
  VIDEOCARDZ: { label: 'VideoCardz', color: '#1e90ff', emoji: '🎮' },
  CNX_SOFTWARE: { label: 'CNX Software', color: '#2e8b57', emoji: '💾' },
};

export const SCORE_COLORS = ['', '#64748b', '#f59e0b', '#3b82f6', '#10b981', '#a855f7'];

export const SCORE_LABELS = ['', '관련성 낮음', '참고 수준', '알아두면 유용', '중요 트렌드', '즉시 적용 가능'];

export function getScoreColor(score: number): string {
  if (score >= 85) return '#a855f7';
  if (score >= 65) return '#10b981';
  if (score >= 45) return '#3b82f6';
  if (score >= 25) return '#f59e0b';
  return '#64748b';
}

export function getScoreLabel(score: number): string {
  if (score >= 85) return '핵심 트렌드';
  if (score >= 65) return '주요 트렌드';
  if (score >= 45) return '알아두면 유용';
  if (score >= 25) return '참고 수준';
  return '관련성 낮음';
}
