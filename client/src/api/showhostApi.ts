import { http } from './http'
import type { ShowhostQnaResponse, ShowhostResponse } from '../types/showhost'

export function fetchShowhost(productCode: string): Promise<ShowhostResponse> {
  return http.get<ShowhostResponse>(`/products/${productCode}/showhost`).then((res) => res.data)
}

export function regenerateShowhost(productCode: string): Promise<ShowhostResponse> {
  return http.post<ShowhostResponse>(`/products/${productCode}/showhost/regenerate`).then((res) => res.data)
}

export function askShowhost(productCode: string, question: string): Promise<ShowhostQnaResponse> {
  return http
    .post<ShowhostQnaResponse>(`/products/${productCode}/showhost/qna`, { question })
    .then((res) => res.data)
}
