import { onMounted, ref } from 'vue'
import { fetchShortlist, generateShortlist } from '../api/shortlistApi'
import type { BestReviewShortlistEntry } from '../types/review'

export function useBestReviewShortlist() {
  const entries = ref<BestReviewShortlistEntry[]>([])
  const loading = ref(false)
  const generating = ref(false)
  const errorMessage = ref('')

  async function refresh() {
    loading.value = true
    try {
      entries.value = await fetchShortlist()
      errorMessage.value = ''
    } catch (error) {
      console.error(error)
      errorMessage.value = '숏리스트를 불러오지 못했습니다.'
    } finally {
      loading.value = false
    }
  }

  async function generate() {
    generating.value = true
    try {
      entries.value = await generateShortlist()
      errorMessage.value = ''
    } catch (error) {
      console.error(error)
      errorMessage.value = '숏리스트 생성에 실패했습니다.'
    } finally {
      generating.value = false
    }
  }

  onMounted(refresh)

  return { entries, loading, generating, errorMessage, refresh, generate }
}
