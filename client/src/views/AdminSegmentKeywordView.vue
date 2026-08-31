<script setup lang="ts">
import { onMounted, ref } from 'vue'
import '../styles/admin.css'
import { fetchSegmentKeywords, suggestSegmentKeywords, updateSegmentKeyword } from '../api/segmentKeywords'
import type { CustomerSegment, SegmentKeywordResponse } from '../api/cs-types'
import { isQuotaExceededError } from '../utils/apiError'
import SegmentKeywordHistoryModal from '../components/SegmentKeywordHistoryModal.vue'

const MIN_REVIEWS_FOR_SUGGESTION = 3

const rows = ref<SegmentKeywordResponse[]>([])
const drafts = ref<Record<string, string>>({})
const loading = ref(false)
const loadError = ref('')
const savingSegment = ref<CustomerSegment | null>(null)
const saveErrorSegment = ref<CustomerSegment | null>(null)

const suggestingSegment = ref<CustomerSegment | null>(null)
const suggestionError = ref<Record<string, string>>({})
const suggestedKeywords = ref<Record<string, string[]>>({})
// 리뷰가 부족해 AI 제안 자체가 불가능했던 세그먼트 — 대신 인터넷에서 직접 검색해볼 검색어를 보여준다.
const reviewShortage = ref<Record<string, boolean>>({})
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
 * 이 세그먼트 고객이 쓴 것으로 확인된 리뷰를 AI로 분석해 키워드 후보를 제안받는다. 결과는
 * 바로 저장되지 않고 화면에 후보로만 표시되며, 관리자가 "+"를 눌러야 입력창에 반영된다.
 */
async function suggestKeywords(row: SegmentKeywordResponse) {
  suggestingSegment.value = row.segment
  suggestionError.value = { ...suggestionError.value, [row.segment]: '' }
  suggestedKeywords.value = { ...suggestedKeywords.value, [row.segment]: [] }
  reviewShortage.value = { ...reviewShortage.value, [row.segment]: false }
  try {
    const result = await suggestSegmentKeywords(row.segment)
    if (result.reviewCount < MIN_REVIEWS_FOR_SUGGESTION) {
      suggestionError.value[row.segment] =
        `분석할 리뷰가 부족합니다 (현재 ${result.reviewCount}건, 최소 ${MIN_REVIEWS_FOR_SUGGESTION}건 필요).`
      reviewShortage.value[row.segment] = true
    } else if (result.keywords.length === 0) {
      suggestionError.value[row.segment] = `리뷰 ${result.reviewCount}건을 분석했지만 새로 제안할 키워드가 없습니다.`
    } else {
      suggestedKeywords.value[row.segment] = result.keywords
    }
  } catch (error) {
    console.error(error)
    suggestionError.value[row.segment] = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과했습니다. 잠시 후 다시 시도해주세요.'
      : '키워드 제안에 실패했습니다.'
  } finally {
    suggestingSegment.value = null
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
  suggestedKeywords.value[row.segment] = (suggestedKeywords.value[row.segment] ?? []).filter((k) => k !== keyword)
}

/** 리뷰가 부족해 AI 분석이 안 될 때, 관리자가 직접 인터넷에서 검색해볼 검색어를 만들어준다. */
function buildSearchQuery(row: SegmentKeywordResponse): string {
  return `${row.segmentLabel} 패션 트렌드 키워드`
}

async function copySearchQuery(row: SegmentKeywordResponse) {
  try {
    await navigator.clipboard.writeText(buildSearchQuery(row))
    copiedSegment.value = row.segment
    setTimeout(() => {
      if (copiedSegment.value === row.segment) copiedSegment.value = null
    }, 1500)
  } catch (error) {
    console.error(error)
  }
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
      "AI 추천 키워드" 버튼은 해당 세그먼트 고객이 쓴 리뷰를 AI로 분석해 키워드 후보를 제안합니다(리뷰
      3건 미만이면 분석하지 않음). 후보를 클릭하면 입력창에 추가만 될 뿐 자동으로 저장되지 않으며, 반영하려면
      "저장" 버튼을 따로 눌러야 합니다. 리뷰가 부족해 분석할 수 없으면, 대신 인터넷에서 검색해볼 수 있는
      검색어를 보여드리니 복사해서 활용하세요.
    </p>

    <p v-if="loading" class="loading">불러오는 중...</p>
    <p v-if="loadError" class="error">{{ loadError }}</p>

    <div v-if="!loading && !loadError" class="segment-groups">
      <div class="segment-group">
        <h3>남성</h3>
        <div v-for="row in maleRows()" :key="row.segment" class="segment-row">
          <label class="segment-label">{{ row.segmentLabel }}</label>
          <textarea v-model="drafts[row.segment]" rows="2" placeholder="예: 가성비, 활동성, 튼튼함"></textarea>
          <div v-if="suggestedKeywords[row.segment]?.length" class="suggestion-chips">
            <span class="suggestion-hint">AI 추천:</span>
            <button
              v-for="keyword in suggestedKeywords[row.segment]"
              :key="keyword"
              type="button"
              class="suggestion-chip"
              @click="applySuggestion(row, keyword)"
            >
              + {{ keyword }}
            </button>
          </div>
          <p v-if="suggestionError[row.segment]" class="suggestion-error">{{ suggestionError[row.segment] }}</p>
          <div v-if="reviewShortage[row.segment]" class="search-hint">
            <span class="search-query">{{ buildSearchQuery(row) }}</span>
            <button type="button" class="copy-button" @click="copySearchQuery(row)">
              {{ copiedSegment === row.segment ? '복사됨' : '복사' }}
            </button>
          </div>
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
          <div v-if="suggestedKeywords[row.segment]?.length" class="suggestion-chips">
            <span class="suggestion-hint">AI 추천:</span>
            <button
              v-for="keyword in suggestedKeywords[row.segment]"
              :key="keyword"
              type="button"
              class="suggestion-chip"
              @click="applySuggestion(row, keyword)"
            >
              + {{ keyword }}
            </button>
          </div>
          <p v-if="suggestionError[row.segment]" class="suggestion-error">{{ suggestionError[row.segment] }}</p>
          <div v-if="reviewShortage[row.segment]" class="search-hint">
            <span class="search-query">{{ buildSearchQuery(row) }}</span>
            <button type="button" class="copy-button" @click="copySearchQuery(row)">
              {{ copiedSegment === row.segment ? '복사됨' : '복사' }}
            </button>
          </div>
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
.suggestion-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}
.suggestion-hint {
  font-size: 11px;
  color: #999;
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
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
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
</style>
