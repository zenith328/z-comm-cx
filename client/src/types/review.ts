export type ReviewStatus = 'PENDING_AI' | 'ANALYZED' | 'FAILED'

export type ReviewClassification = 'NONE' | 'RECOMMENDED' | 'BEST_CANDIDATE'

export type ReviewClassificationSource = 'AI' | 'ADMIN'

export type ReviewSentiment = 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE'

export type ReviewSortOption = 'LATEST' | 'RATING_HIGH' | 'RATING_LOW' | 'POSITIVE_FIRST' | 'NEGATIVE_FIRST'

export type ReviewOrigin = 'NATIVE' | 'EXTERNAL' | 'SYNTHETIC'

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

export type FitLevel = 'TIGHT' | 'TRUE_TO_SIZE' | 'LOOSE' | 'UNKNOWN'

export interface FitProfileResponse {
  axis1Label: string
  axis2Label: string
  axis3Label: string
  shoulderFit: FitLevel
  chestFit: FitLevel
  lengthFit: FitLevel
  recommendedBodyType: string
  summary: string
  basedOnReviewCount: number
  fromColdStartFallback: boolean
}

export interface SyntheticReviewSeedRequest {
  productCode: string
  productName: string
  brand: string | null
  category: string | null
  description: string | null
  count: number
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
