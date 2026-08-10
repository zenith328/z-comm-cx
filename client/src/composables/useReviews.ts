import { computed, onMounted, onUnmounted, ref } from 'vue'
import {
  createReview as apiCreateReview,
  fetchReviews,
  overrideClassification as apiOverrideClassification,
  reanalyzeReview as apiReanalyzeReview,
} from '../api/reviewApi'
import type {
  Review,
  ReviewClassification,
  ReviewCreateRequest,
  ReviewOverrideRequest,
  ReviewStatus,
} from '../types/review'

const POLL_INTERVAL_MS = 2000
const PAGE_SIZE = 10

export interface ReviewFilters {
  productCode: string
  visible: boolean | 'ALL'
  classification: ReviewClassification | 'ALL'
  status: ReviewStatus | 'ALL'
}

export function useReviews() {
  const reviews = ref<Review[]>([])
  const loading = ref(false)
  const errorMessage = ref('')
  const page = ref(0)
  const totalPages = ref(1)
  const totalElements = ref(0)
  const filters = ref<ReviewFilters>({ productCode: '', visible: 'ALL', classification: 'ALL', status: 'ALL' })
  let pollTimer: ReturnType<typeof setInterval> | undefined

  const hasPendingAnalysis = computed(() =>
    reviews.value.some((review) => review.status === 'PENDING_AI'),
  )

  async function refresh() {
    loading.value = true
    try {
      const result = await fetchReviews({
        page: page.value,
        size: PAGE_SIZE,
        productCode: filters.value.productCode.trim() || undefined,
        visible: filters.value.visible === 'ALL' ? undefined : filters.value.visible,
        classification: filters.value.classification === 'ALL' ? undefined : filters.value.classification,
        status: filters.value.status === 'ALL' ? undefined : filters.value.status,
      })
      reviews.value = result.content
      totalPages.value = result.totalPages
      totalElements.value = result.totalElements
      errorMessage.value = ''
    } catch (error) {
      console.error(error)
      errorMessage.value = '리뷰 목록을 불러오지 못했습니다. 백엔드(8080)가 실행 중인지 확인하세요.'
    } finally {
      loading.value = false
    }
  }

  async function submitReview(request: ReviewCreateRequest) {
    await apiCreateReview(request)
    page.value = 0
    await refresh()
    ensurePolling()
  }

  async function overrideClassification(id: number, request: ReviewOverrideRequest) {
    await apiOverrideClassification(id, request)
    await refresh()
  }

  async function reanalyze(id: number) {
    await apiReanalyzeReview(id)
    await refresh()
    ensurePolling()
  }

  function updateFilters(partial: Partial<ReviewFilters>) {
    filters.value = { ...filters.value, ...partial }
    page.value = 0
    refresh()
  }

  function goToPage(target: number) {
    if (target < 0 || target >= totalPages.value) return
    page.value = target
    refresh()
  }

  function ensurePolling() {
    if (pollTimer) return
    pollTimer = setInterval(() => {
      if (hasPendingAnalysis.value) {
        refresh()
      } else {
        clearInterval(pollTimer)
        pollTimer = undefined
      }
    }, POLL_INTERVAL_MS)
  }

  onMounted(async () => {
    await refresh()
    if (hasPendingAnalysis.value) ensurePolling()
  })

  onUnmounted(() => {
    if (pollTimer) clearInterval(pollTimer)
  })

  return {
    reviews,
    loading,
    errorMessage,
    page,
    totalPages,
    totalElements,
    filters,
    refresh,
    submitReview,
    overrideClassification,
    reanalyze,
    updateFilters,
    goToPage,
    ensurePolling,
  }
}
