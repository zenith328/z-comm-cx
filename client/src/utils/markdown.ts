function escapeHtml(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

// AI 응답에 섞여 나올 수 있는 markdown(**굵게**, `코드`) 최소 렌더링. HTML은 먼저 이스케이프한다.
export function renderMarkdownLite(text: string): string {
  return escapeHtml(text)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
}
