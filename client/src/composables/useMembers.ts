import { onMounted, ref } from 'vue'
import { fetchMembers } from '../api/members'
import type { MemberResponse } from '../api/cs-types'

const DEFAULT_PAGE_SIZE = 10

export function useMembers(options: { pageSize?: number } = {}) {
  const pageSize = options.pageSize ?? DEFAULT_PAGE_SIZE
  const members = ref<MemberResponse[]>([])
  const loading = ref(false)
  const errorMessage = ref('')
  const page = ref(0)
  const totalPages = ref(1)
  const totalElements = ref(0)
  const searchQuery = ref('')

  async function refresh() {
    loading.value = true
    try {
      const result = await fetchMembers({
        page: page.value,
        size: pageSize,
        search: searchQuery.value.trim() || undefined,
      })
      members.value = result.content
      totalPages.value = result.totalPages
      totalElements.value = result.totalElements
      errorMessage.value = ''
    } catch (error) {
      console.error(error)
      errorMessage.value = '회원 목록을 불러오지 못했습니다.'
    } finally {
      loading.value = false
    }
  }

  function goToPage(target: number) {
    if (target < 0 || target >= totalPages.value) return
    page.value = target
    refresh()
  }

  function setSearchQuery(value: string) {
    searchQuery.value = value
    page.value = 0
    refresh()
  }

  onMounted(() => {
    refresh()
  })

  return {
    members,
    loading,
    errorMessage,
    page,
    totalPages,
    totalElements,
    searchQuery,
    refresh,
    goToPage,
    setSearchQuery,
  }
}
