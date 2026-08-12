import { http } from './http'

export interface DailyUsageResponse {
  date: string
  koreaRangeStart: string
  koreaRangeEnd: string
  requestCount: number
  tokenCount: number
}

export interface GeminiApiUsageResponse {
  tier: string
  rpmLimit: number
  tpmLimit: number
  limitPerDay: number
  today: DailyUsageResponse
  recent: DailyUsageResponse[]
}

export function fetchGeminiUsage() {
  return http.get<GeminiApiUsageResponse>('/admin/gemini-usage').then((res) => res.data)
}
