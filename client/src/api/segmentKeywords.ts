import { http } from './http'
import type {
  CustomerSegment,
  SegmentKeywordHistoryResponse,
  SegmentKeywordRequest,
  SegmentKeywordResponse,
  SegmentKeywordSuggestionResponse,
} from './cs-types'

export function fetchSegmentKeywords() {
  return http.get<SegmentKeywordResponse[]>('/segment-keywords').then((res) => res.data)
}

export function updateSegmentKeyword(segment: CustomerSegment, keywords: string) {
  const body: SegmentKeywordRequest = { keywords }
  return http.put<SegmentKeywordResponse>(`/segment-keywords/${segment}`, body).then((res) => res.data)
}

export function suggestSegmentKeywords(segment: CustomerSegment) {
  return http
    .post<SegmentKeywordSuggestionResponse>(`/segment-keywords/${segment}/suggest-keywords`)
    .then((res) => res.data)
}

export function fetchSegmentKeywordHistory(segment: CustomerSegment) {
  return http.get<SegmentKeywordHistoryResponse[]>(`/segment-keywords/${segment}/history`).then((res) => res.data)
}
