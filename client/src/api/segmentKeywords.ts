import { http } from './http'
import type {
  CustomerSegment,
  SegmentKeywordHistoryItem,
  SegmentKeywordRequest,
  SegmentKeywordResponse,
  SegmentKeywordSuggestionResponse,
} from './cs-types'
import type { PageResponse } from '../types/page'

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

export function fetchSegmentKeywordHistory(segment: CustomerSegment, page: number, size: number) {
  return http
    .get<PageResponse<SegmentKeywordHistoryItem>>(`/segment-keywords/${segment}/history`, { params: { page, size } })
    .then((res) => res.data)
}
