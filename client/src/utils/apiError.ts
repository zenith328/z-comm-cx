import axios from 'axios'

/** 백엔드가 Gemini 429(사용량 한도 초과)를 감지하면 HTTP 429로 내려준다. */
export function isQuotaExceededError(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 429
}
