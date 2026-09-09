import axios from 'axios'

/** 백엔드가 Gemini 429(사용량 한도 초과)를 감지하면 HTTP 429로 내려준다. */
export function isQuotaExceededError(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 429
}

/**
 * GlobalExceptionHandler가 내려주는 { message } 바디(예: CX-Pay 잔액부족, 재고부족 등
 * IllegalStateException/IllegalArgumentException 메시지)를 그대로 보여준다. 그 형태가 아니면
 * fallback을 대신 보여준다.
 */
export function extractErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const message = (error.response?.data as { message?: string } | undefined)?.message
    if (message) return message
  }
  return fallback
}
