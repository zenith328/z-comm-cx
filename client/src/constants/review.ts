import type { ReviewClassification, ReviewSentiment, ReviewStatus } from '../types/review'

export const CLASSIFICATION_LABELS: Record<ReviewClassification, string> = {
  NONE: '일반',
  RECOMMENDED: '추천',
  BEST_CANDIDATE: '베스트',
}

export const CLASSIFICATION_OPTIONS = Object.keys(CLASSIFICATION_LABELS) as ReviewClassification[]

export const STATUS_LABELS: Record<ReviewStatus, string> = {
  PENDING_AI: 'AI 분석 중',
  ANALYZED: '분석 완료',
  FAILED: '분석 실패',
}

export const VISIBILITY_LABELS: Record<'true' | 'false', string> = {
  true: '공개',
  false: '비공개',
}

export function visibilityLabel(visible: boolean): string {
  return VISIBILITY_LABELS[visible ? 'true' : 'false']
}

export const SENTIMENT_LABELS: Record<ReviewSentiment, string> = {
  POSITIVE: '긍정',
  NEUTRAL: '중립',
  NEGATIVE: '부정',
}

export const SENTIMENT_OPTIONS = Object.keys(SENTIMENT_LABELS) as ReviewSentiment[]
