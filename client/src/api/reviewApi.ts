import axios from 'axios'
import type { Review, ReviewClassification, ReviewCreateRequest, ReviewOverrideRequest, ReviewStatus } from '../types/review'
import type { PageResponse } from '../types/page'
import { attachSiteAuthInterceptor } from '../stores/siteAuth'
import { attachColdStartIndicator } from '../stores/coldStart'

const client = axios.create({ baseURL: '/api' })
attachSiteAuthInterceptor(client)
attachColdStartIndicator(client)

export interface FetchReviewsParams {
  page: number
  size: number
  productCode?: string
  visible?: boolean
  classification?: ReviewClassification
  status?: ReviewStatus
}

export function fetchReviews(params: FetchReviewsParams): Promise<PageResponse<Review>> {
  return client.get<PageResponse<Review>>('/reviews', { params }).then((res) => res.data)
}

export function createReview(request: ReviewCreateRequest): Promise<Review> {
  return client.post<Review>('/reviews', request).then((res) => res.data)
}

export function overrideClassification(id: number, request: ReviewOverrideRequest): Promise<Review> {
  return client.patch<Review>(`/reviews/${id}/classification`, request).then((res) => res.data)
}

export function reanalyzeReview(id: number): Promise<Review> {
  return client.post<Review>(`/reviews/${id}/reanalyze`).then((res) => res.data)
}
