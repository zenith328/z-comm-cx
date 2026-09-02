<script setup lang="ts">
import { onMounted, ref } from 'vue'
import '../styles/admin.css'
import { fetchSegmentKeywords, suggestSegmentKeywords, updateSegmentKeyword } from '../api/segmentKeywords'
import type { CustomerSegment, SegmentKeywordResponse } from '../api/cs-types'
import { isQuotaExceededError } from '../utils/apiError'
import SegmentKeywordHistoryModal from '../components/SegmentKeywordHistoryModal.vue'

const rows = ref<SegmentKeywordResponse[]>([])
const drafts = ref<Record<string, string>>({})
const loading = ref(false)
const loadError = ref('')
const savingSegment = ref<CustomerSegment | null>(null)
const saveErrorSegment = ref<CustomerSegment | null>(null)

const suggestingSegment = ref<CustomerSegment | null>(null)
const suggestionError = ref<Record<string, string>>({})
// "AI 추천 키워드"를 누르면 항상 세 가지(리뷰 기반/일반지식 기반/검색어)를 함께 보여준다.
const hasSuggested = ref<Record<string, boolean>>({})
const reviewSuggestions = ref<Record<string, string[]>>({})
const reviewCounts = ref<Record<string, number>>({})
const generalSuggestions = ref<Record<string, string[]>>({})
const searchQueryText = ref<Record<string, string>>({})
const copiedSegment = ref<CustomerSegment | null>(null)

const historyModalRow = ref<SegmentKeywordResponse | null>(null)

function openHistory(row: SegmentKeywordResponse) {
  historyModalRow.value = row
}

function closeHistory() {
  historyModalRow.value = null
}

const maleRows = () => rows.value.filter((row) => row.gender === 'MALE')
const femaleRows = () => rows.value.filter((row) => row.gender === 'FEMALE')

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    rows.value = await fetchSegmentKeywords()
    drafts.value = Object.fromEntries(rows.value.map((row) => [row.segment, row.keywords ?? '']))
  } catch (error) {
    console.error(error)
    loadError.value = '세그먼트 키워드를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function isDirty(row: SegmentKeywordResponse): boolean {
  return (drafts.value[row.segment] ?? '') !== (row.keywords ?? '')
}

async function save(row: SegmentKeywordResponse) {
  savingSegment.value = row.segment
  saveErrorSegment.value = null
  try {
    const updated = await updateSegmentKeyword(row.segment, drafts.value[row.segment] ?? '')
    const index = rows.value.findIndex((item) => item.segment === row.segment)
    if (index !== -1) rows.value[index] = updated
  } catch (error) {
    console.error(error)
    saveErrorSegment.value = row.segment
  } finally {
    savingSegment.value = null
  }
}

function formatUpdatedAt(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 16) + ' 저장됨' : '미입력'
}

/**
 * "AI 추천 키워드"를 누르면 항상 세 가지를 함께 받는다: ①리뷰 기반 키워드(리뷰가 부족하면 빈
 * 배열), ②AI 일반 지식 기반 키워드, ③관리자가 직접 검색해볼 검색어. 어느 것도 자동으로
 * 저장되지 않고, 후보 키워드를 클릭하면 입력창에 추가만 되며 "저장"은 별도로 눌러야 한다.
 */
async function suggestKeywords(row: SegmentKeywordResponse) {
  suggestingSegment.value = row.segment
  suggestionError.value = { ...suggestionError.value, [row.segment]: '' }
  try {
    const result = await suggestSegmentKeywords(row.segment)
    reviewCounts.value = { ...reviewCounts.value, [row.segment]: result.reviewCount }
    reviewSuggestions.value = { ...reviewSuggestions.value, [row.segment]: result.reviewKeywords }
    generalSuggestions.value = { ...generalSuggestions.value, [row.segment]: result.generalKeywords }
    searchQueryText.value = { ...searchQueryText.value, [row.segment]: result.searchQuery ?? '' }
    hasSuggested.value = { ...hasSuggested.value, [row.segment]: true }
  } catch (error) {
    console.error(error)
    suggestionError.value[row.segment] = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과했습니다. 잠시 후 다시 시도해주세요.'
      : '키워드 제안에 실패했습니다.'
  } finally {
    suggestingSegment.value = null
  }
}

async function copySearchQuery(row: SegmentKeywordResponse) {
  const text = searchQueryText.value[row.segment]
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    copiedSegment.value = row.segment
    setTimeout(() => {
      if (copiedSegment.value === row.segment) copiedSegment.value = null
    }, 1500)
  } catch (error) {
    console.error(error)
  }
}

/** 후보 키워드 하나를 입력창(쉼표 구분 텍스트)에 중복 없이 추가한다. 저장은 별도로 눌러야 한다. */
function applySuggestion(row: SegmentKeywordResponse, keyword: string) {
  const parts = (drafts.value[row.segment] ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  if (!parts.includes(keyword)) {
    parts.push(keyword)
  }
  drafts.value[row.segment] = parts.join(', ')
  reviewSuggestions.value[row.segment] = (reviewSuggestions.value[row.segment] ?? []).filter((k) => k !== keyword)
  generalSuggestions.value[row.segment] = (generalSuggestions.value[row.segment] ?? []).filter((k) => k !== keyword)
}

onMounted(load)
</script>

<template>
  <div>
    <RouterLink to="/admin/products" class="back-link">← 상품관리로 돌아가기</RouterLink>

    <h2 class="admin-title">성향키워드 관리</h2>

    <p class="subtitle">
      성별×연령 세그먼트별로 강조할 키워드/포인트입니다. 상품마다 따로 입력하지 않고 전체 상품에 공통으로
      적용되며, 상품상세설명을 AI로 생성할 때 이 키워드를 함께 참고합니다.
    </p>
    <p class="hint">
      "AI 추천 키워드" 버튼을 누르면 항상 세 가지를 함께 보여줍니다 — ①리뷰 기반 키워드(리뷰 3건
      미만이면 "리뷰 부족"으로 표시), ②AI 일반 지식 기반 키워드(참고용), ③관리자가 직접 검색해볼
      검색어. 키워드 후보를 클릭하면 입력창에 추가만 될 뿐 자동으로 저장되지 않으며, 반영하려면
      "저장" 버튼을 따로 눌러야 합니다.
    </p>

    <p v-if="loading" class="loading">불러오는 중...</p>
    <p v-if="loadError" class="error">{{ loadError }}</p>

    <div v-if="!loading && !loadError" class="segment-groups">
      <div class="segment-group">
        <h3>남성</h3>
        <div v-for="row in maleRows()" :key="row.segment" class="segment-row">
          <label class="segment-label">{{ row.segmentLabel }}</label>
          <textarea v-model="drafts[row.segment]" rows="2" placeholder="예: 가성비, 활동성, 튼튼함"></textarea>
          <div v-if="hasSuggested[row.segment]" class="suggestion-panel">
            <div class="suggestion-row">
              <span class="suggestion-hint">AI 추천(리뷰):</span>
              <span v-if="reviewCounts[row.segment] < 3" class="suggestion-note">
                리뷰 부족 (현재 {{ reviewCounts[row.segment] }}건)
              </span>
              <template v-else-if="reviewSuggestions[row.segment]?.length">
                <button
                  v-for="keyword in reviewSuggestions[row.segment]"
                  :key="keyword"
                  type="button"
                  class="suggestion-chip"
                  @click="applySuggestion(row, keyword)"
                >
                  + {{ keyword }}
                </button>
              </template>
              <span v-else class="suggestion-note">제안할 키워드 없음</span>
            </div>
            <div class="suggestion-row">
              <span class="suggestion-hint">AI 추천(일반지식):</span>
              <template v-if="generalSuggestions[row.segment]?.length">
                <button
                  v-for="keyword in generalSuggestions[row.segment]"
                  :key="keyword"
                  type="button"
                  class="suggestion-chip"
                  @click="applySuggestion(row, keyword)"
                >
                  + {{ keyword }}
                </button>
              </template>
              <span v-else class="suggestion-note">제안 없음</span>
            </div>
            <div class="suggestion-row search-hint">
              <span class="suggestion-hint">AI 추천 검색어:</span>
              <span class="search-query">{{ searchQueryText[row.segment] || '(생성 실패)' }}</span>
              <button
                type="button"
                class="copy-button"
                :disabled="!searchQueryText[row.segment]"
                @click="copySearchQuery(row)"
              >
                {{ copiedSegment === row.segment ? '복사됨' : '복사' }}
              </button>
            </div>
          </div>
          <p v-if="suggestionError[row.segment]" class="suggestion-error">{{ suggestionError[row.segment] }}</p>
          <div class="segment-row-footer">
            <span class="status">{{ formatUpdatedAt(row.updatedAt) }}</span>
            <span v-if="saveErrorSegment === row.segment" class="error">저장 실패</span>
            <button
              type="button"
              class="suggest-button"
              :disabled="suggestingSegment === row.segment"
              @click="suggestKeywords(row)"
            >
              {{ suggestingSegment === row.segment ? '분석 중...' : 'AI 추천 키워드' }}
            </button>
            <button type="button" :disabled="!isDirty(row) || savingSegment === row.segment" @click="save(row)">
              {{ savingSegment === row.segment ? '저장 중...' : '저장' }}
            </button>
            <button type="button" class="history-button" @click="openHistory(row)">이력보기</button>
          </div>
        </div>
      </div>

      <div class="segment-group">
        <h3>여성</h3>
        <div v-for="row in femaleRows()" :key="row.segment" class="segment-row">
          <label class="segment-label">{{ row.segmentLabel }}</label>
          <textarea v-model="drafts[row.segment]" rows="2" placeholder="예: 트렌디함, 디자인, 선물용"></textarea>
          <div v-if="hasSuggested[row.segment]" class="suggestion-panel">
            <div class="suggestion-row">
              <span class="suggestion-hint">AI 추천(리뷰):</span>
              <span v-if="reviewCounts[row.segment] < 3" class="suggestion-note">
                리뷰 부족 (현재 {{ reviewCounts[row.segment] }}건)
              </span>
              <template v-else-if="reviewSuggestions[row.segment]?.length">
                <button
                  v-for="keyword in reviewSuggestions[row.segment]"
                  :key="keyword"
                  type="button"
                  class="suggestion-chip"
                  @click="applySuggestion(row, keyword)"
                >
                  + {{ keyword }}
                </button>
              </template>
              <span v-else class="suggestion-note">제안할 키워드 없음</span>
            </div>
            <div class="suggestion-row">
              <span class="suggestion-hint">AI 추천(일반지식):</span>
              <template v-if="generalSuggestions[row.segment]?.length">
                <button
                  v-for="keyword in generalSuggestions[row.segment]"
                  :key="keyword"
                  type="button"
                  class="suggestion-chip"
                  @click="applySuggestion(row, keyword)"
                >
                  + {{ keyword }}
                </button>
              </template>
              <span v-else class="suggestion-note">제안 없음</span>
            </div>
            <div class="suggestion-row search-hint">
              <span class="suggestion-hint">AI 추천 검색어:</span>
              <span class="search-query">{{ searchQueryText[row.segment] || '(생성 실패)' }}</span>
              <button
                type="button"
                class="copy-button"
                :disabled="!searchQueryText[row.segment]"
                @click="copySearchQuery(row)"
              >
                {{ copiedSegment === row.segment ? '복사됨' : '복사' }}
              </button>
            </div>
          </div>
          <p v-if="suggestionError[row.segment]" class="suggestion-error">{{ suggestionError[row.segment] }}</p>
          <div class="segment-row-footer">
            <span class="status">{{ formatUpdatedAt(row.updatedAt) }}</span>
            <span v-if="saveErrorSegment === row.segment" class="error">저장 실패</span>
            <button
              type="button"
              class="suggest-button"
              :disabled="suggestingSegment === row.segment"
              @click="suggestKeywords(row)"
            >
              {{ suggestingSegment === row.segment ? '분석 중...' : 'AI 추천 키워드' }}
            </button>
            <button type="button" :disabled="!isDirty(row) || savingSegment === row.segment" @click="save(row)">
              {{ savingSegment === row.segment ? '저장 중...' : '저장' }}
            </button>
            <button type="button" class="history-button" @click="openHistory(row)">이력보기</button>
          </div>
        </div>
      </div>
    </div>

    <SegmentKeywordHistoryModal
      :open="historyModalRow !== null"
      :segment="historyModalRow?.segment ?? null"
      :segment-label="historyModalRow?.segmentLabel ?? ''"
      @close="closeHistory"
    />
  </div>
</template>

<style scoped>
.back-link {
  display: inline-block;
  margin-bottom: 16px;
  font-size: 13px;
  color: #0056b3;
  text-decoration: none;
}
.back-link:hover {
  text-decoration: underline;
}
.subtitle {
  margin: 0 0 8px;
  color: #666;
  font-size: 14px;
}
.hint {
  margin: 0 0 20px;
  color: #999;
  font-size: 12px;
}
.loading,
.error {
  font-size: 13px;
}
.error {
  color: #a80000;
}
.segment-groups {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
}
.segment-group h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: #333;
}
.segment-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}
.segment-label {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}
.segment-row textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 8px 10px;
  font-size: 13px;
  border: 1px solid #ccc;
  border-radius: 4px;
  resize: vertical;
}
.segment-row-footer {
  display: flex;
  align-items: center;
  gap: 8px;
}
.segment-row-footer .status {
  font-size: 11px;
  color: #999;
  flex: 1;
}
.segment-row-footer button {
  padding: 5px 12px;
  font-size: 12px;
  border: 1px solid #0056b3;
  border-radius: 4px;
  background: #0056b3;
  color: #fff;
  cursor: pointer;
}
.segment-row-footer button:disabled {
  background: #ccc;
  border-color: #ccc;
  cursor: default;
}
.segment-row-footer .suggest-button {
  background: #fff;
  color: #0056b3;
}
.segment-row-footer .suggest-button:disabled {
  color: #999;
  border-color: #ccc;
  background: #fff;
}
.segment-row-footer .history-button {
  background: #fff;
  color: #555;
  border-color: #ccc;
}
.segment-row-footer .history-button:hover {
  background: #f5f5f5;
}
.suggestion-panel {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 2px;
}
.suggestion-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.suggestion-hint {
  font-size: 11px;
  color: #999;
  flex-shrink: 0;
}
.suggestion-note {
  font-size: 11px;
  color: #bbb;
}
.suggestion-chip {
  padding: 3px 8px;
  font-size: 11px;
  border: 1px dashed #0056b3;
  border-radius: 12px;
  background: #eef4ff;
  color: #0056b3;
  cursor: pointer;
}
.suggestion-chip:hover {
  background: #dceaff;
}
.suggestion-error {
  margin: 2px 0 0;
  font-size: 11px;
  color: #a80000;
}
.search-hint {
  padding: 6px 10px;
  border: 1px dashed #ccc;
  border-radius: 6px;
  background: #fafafa;
}
.search-query {
  flex: 1;
  font-size: 12px;
  color: #333;
}
.copy-button {
  padding: 3px 10px;
  font-size: 11px;
  border: 1px solid #0056b3;
  border-radius: 4px;
  background: #fff;
  color: #0056b3;
  cursor: pointer;
}
.copy-button:hover {
  background: #eef4ff;
}
.copy-button:disabled {
  color: #999;
  border-color: #ccc;
  cursor: default;
}
</style>
