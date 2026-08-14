export type ReviewStatus = 'PENDING_AI' | 'ANALYZED' | 'FAILED'

export type ReviewClassification = 'NONE' | 'RECOMMENDED' | 'BEST_CANDIDATE'

export type ReviewClassificationSource = 'AI' | 'ADMIN'

export type ReviewSentiment = 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE'

export type ReviewSortOption = 'LATEST' | 'RATING_HIGH' | 'RATING_LOW' | 'POSITIVE_FIRST' | 'NEGATIVE_FIRST'

export interface Review {
  id: number
  productCode: string
  memberId: string
  content: string
  rating: number
  hasPhoto: boolean
  createdAt: string
  status: ReviewStatus
  visible: boolean
  classification: ReviewClassification
  classificationSource: ReviewClassificationSource
  sentiment: ReviewSentiment | null
  riskScore: number | null
  aiReason: string | null
  overrideNote: string | null
  overriddenAt: string | null
}

export interface ReviewCreateRequest {
  productCode: string
  memberId: string
  memberPhone?: string
  content: string
  rating: number
  hasPhoto: boolean
}

export interface ReviewOverrideRequest {
  visible: boolean
  classification: ReviewClassification
  note: string | null
}

export interface ClientReview {
  id: number
  memberId: string
  rating: number
  hasPhoto: boolean
  content: string
  createdAt: string
  classification: ReviewClassification
  sentiment: ReviewSentiment | null
}

export interface ReviewSummaryResponse {
  summary: string
  reviewCount: number
}

export interface BestReviewShortlistEntry {
  weekLabel: string
  productCode: string
  rank: number
  review: Review
}

export interface ClientBestReviewShortlistEntry {
  weekLabel: string
  rank: number
  review: ClientReview
}
