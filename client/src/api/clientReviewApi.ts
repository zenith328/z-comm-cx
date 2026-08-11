import axios from 'axios'
import type {
  ClientBestReviewShortlistEntry,
  ClientReview,
  ReviewClassification,
  ReviewSentiment,
  ReviewSortOption,
  ReviewSummaryResponse,
} from '../types/review'
import type { PageResponse } from '../types/page'
import { attachSiteAuthInterceptor } from '../stores/siteAuth'

const client = axios.create({ baseURL: '/api' })
attachSiteAuthInterceptor(client)

export interface FetchVisibleReviewsParams {
  page: number
  size: number
  hasPhoto?: boolean
  classification?: ReviewClassification
  sentiment?: ReviewSentiment
  sort?: ReviewSortOption
}

export function fetchVisibleReviews(
  productCode: string,
  params: FetchVisibleReviewsParams,
): Promise<PageResponse<ClientReview>> {
  return client
    .get<PageResponse<ClientReview>>(`/products/${productCode}/reviews`, { params })
    .then((res) => res.data)
}

export function summarizeReviews(productCode: string, query: string): Promise<ReviewSummaryResponse> {
  return client
    .post<ReviewSummaryResponse>(`/products/${productCode}/reviews/summary`, { query })
    .then((res) => res.data)
}

export function fetchBestReviewShortlist(productCode: string): Promise<ClientBestReviewShortlistEntry[]> {
  return client
    .get<ClientBestReviewShortlistEntry[]>(`/products/${productCode}/best-review-shortlist`)
    .then((res) => res.data)
}
