import axios from 'axios'
import type {
  ClientBestReviewShortlistEntry,
  ClientReview,
  FitProfileResponse,
  ReviewClassification,
  ReviewSentiment,
  ReviewSortOption,
  ReviewSummaryResponse,
} from '../types/review'
import type { Gender } from './cs-types'
import type { PageResponse } from '../types/page'
import { attachSiteAuthInterceptor } from '../stores/siteAuth'
import { attachColdStartIndicator } from '../stores/coldStart'

const client = axios.create({ baseURL: '/api' })
attachSiteAuthInterceptor(client)
attachColdStartIndicator(client)

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

// viewerGender를 주면 그 성별 고객 기준으로 걸러낸 요약을 받는다(비로그인/미입력이면 null → 일반 요약).
export function summarizeReviews(
  productCode: string,
  query: string,
  viewerGender: Gender | null,
): Promise<ReviewSummaryResponse> {
  return client
    .post<ReviewSummaryResponse>(`/products/${productCode}/reviews/summary`, { query, viewerGender })
    .then((res) => res.data)
}

export function fetchBestReviewShortlist(productCode: string): Promise<ClientBestReviewShortlistEntry[]> {
  return client
    .get<ClientBestReviewShortlistEntry[]>(`/products/${productCode}/best-review-shortlist`)
    .then((res) => res.data)
}

export function fetchFitProfile(productCode: string): Promise<FitProfileResponse> {
  return client.get<FitProfileResponse>(`/products/${productCode}/reviews/fit-profile`).then((res) => res.data)
}
