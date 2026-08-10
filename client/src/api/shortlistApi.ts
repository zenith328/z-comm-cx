import axios from 'axios'
import type { BestReviewShortlistEntry } from '../types/review'

const client = axios.create({ baseURL: '/api' })

export function generateShortlist(): Promise<BestReviewShortlistEntry[]> {
  return client.post<BestReviewShortlistEntry[]>('/best-review-shortlist/generate').then((res) => res.data)
}

export function fetchShortlist(): Promise<BestReviewShortlistEntry[]> {
  return client.get<BestReviewShortlistEntry[]>('/best-review-shortlist').then((res) => res.data)
}
