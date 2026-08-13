<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchSegmentKeywords, updateSegmentKeyword } from '../api/segmentKeywords'
import type { CustomerSegment, SegmentKeywordResponse } from '../api/cs-types'

const rows = ref<SegmentKeywordResponse[]>([])
const drafts = ref<Record<string, string>>({})
const loading = ref(false)
const loadError = ref('')
const savingSegment = ref<CustomerSegment | null>(null)
const saveErrorSegment = ref<CustomerSegment | null>(null)

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

onMounted(load)
</script>

<template>
  <div>
    <RouterLink to="/admin/products" class="back-link">← 상품관리로 돌아가기</RouterLink>

    <p class="subtitle">
      성별×연령 세그먼트별로 강조할 키워드/포인트입니다. 상품마다 따로 입력하지 않고 전체 상품에 공통으로
      적용되며, 상품상세설명을 AI로 생성할 때 이 키워드를 함께 참고합니다.
    </p>

    <p v-if="loading" class="loading">불러오는 중...</p>
    <p v-if="loadError" class="error">{{ loadError }}</p>

    <div v-if="!loading && !loadError" class="segment-groups">
      <div class="segment-group">
        <h3>남성</h3>
        <div v-for="row in maleRows()" :key="row.segment" class="segment-row">
          <label class="segment-label">{{ row.segmentLabel }}</label>
          <textarea v-model="drafts[row.segment]" rows="2" placeholder="예: 가성비, 활동성, 튼튼함"></textarea>
          <div class="segment-row-footer">
            <span class="status">{{ formatUpdatedAt(row.updatedAt) }}</span>
            <span v-if="saveErrorSegment === row.segment" class="error">저장 실패</span>
            <button type="button" :disabled="!isDirty(row) || savingSegment === row.segment" @click="save(row)">
              {{ savingSegment === row.segment ? '저장 중...' : '저장' }}
            </button>
          </div>
        </div>
      </div>

      <div class="segment-group">
        <h3>여성</h3>
        <div v-for="row in femaleRows()" :key="row.segment" class="segment-row">
          <label class="segment-label">{{ row.segmentLabel }}</label>
          <textarea v-model="drafts[row.segment]" rows="2" placeholder="예: 트렌디함, 디자인, 선물용"></textarea>
          <div class="segment-row-footer">
            <span class="status">{{ formatUpdatedAt(row.updatedAt) }}</span>
            <span v-if="saveErrorSegment === row.segment" class="error">저장 실패</span>
            <button type="button" :disabled="!isDirty(row) || savingSegment === row.segment" @click="save(row)">
              {{ savingSegment === row.segment ? '저장 중...' : '저장' }}
            </button>
          </div>
        </div>
      </div>
    </div>
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
  margin: 0 0 20px;
  color: #666;
  font-size: 14px;
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
</style>
