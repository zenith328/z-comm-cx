<script setup lang="ts">
import { ref, watch } from 'vue'
import Pagination from '../components/Pagination.vue'
import BestReviewShortlistPanel from '../components/BestReviewShortlistPanel.vue'
import ReviewFilterBar from '../components/ReviewFilterBar.vue'
import ReviewTable from '../components/ReviewTable.vue'
import { useReviews } from '../composables/useReviews'
import type { ReviewClassification, ReviewOverrideRequest, ReviewStatus } from '../types/review'

const {
  reviews,
  loading,
  errorMessage,
  page,
  totalPages,
  totalElements,
  refresh,
  overrideClassification,
  reanalyze,
  updateFilters,
  goToPage,
} = useReviews()

const productCodeFilter = ref('')
const visibleFilter = ref<boolean | 'ALL'>('ALL')
const classificationFilter = ref<ReviewClassification | 'ALL'>('ALL')
const statusFilter = ref<ReviewStatus | 'ALL'>('ALL')
const overridingId = ref<number | null>(null)
const reanalyzingId = ref<number | null>(null)

let productCodeDebounceTimer: ReturnType<typeof setTimeout> | undefined
watch(productCodeFilter, (value) => {
  clearTimeout(productCodeDebounceTimer)
  productCodeDebounceTimer = setTimeout(() => updateFilters({ productCode: value }), 300)
})
watch(visibleFilter, (value) => updateFilters({ visible: value }))
watch(classificationFilter, (value) => updateFilters({ classification: value }))
watch(statusFilter, (value) => updateFilters({ status: value }))

async function handleOverride(id: number, request: ReviewOverrideRequest) {
  overridingId.value = id
  try {
    await overrideClassification(id, request)
  } finally {
    overridingId.value = null
  }
}

async function handleReanalyze(id: number) {
  reanalyzingId.value = id
  try {
    await reanalyze(id)
  } finally {
    reanalyzingId.value = null
  }
}
</script>

<template>
  <div>
    <p class="subtitle">AI가 리뷰의 공개여부(공개/비공개)와 분류(일반/추천/베스트)를 자동으로 판단합니다.</p>

    <BestReviewShortlistPanel />

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div class="list-toolbar">
      <ReviewFilterBar
        v-model:product-code="productCodeFilter"
        v-model:visible="visibleFilter"
        v-model:classification="classificationFilter"
        v-model:status="statusFilter"
      />
      <div class="list-toolbar-right">
        <span class="total-count">총 {{ totalElements }}건</span>
        <p v-if="loading" class="loading">불러오는 중...</p>
        <button type="button" class="refresh-button" :disabled="loading" @click="refresh">새로고침</button>
      </div>
    </div>
    <ReviewTable
      :reviews="reviews"
      :overriding-id="overridingId"
      :reanalyzing-id="reanalyzingId"
      @override="handleOverride"
      @reanalyze="handleReanalyze"
    />

    <Pagination :page="page" :total-pages="totalPages" :total-elements="totalElements" @change="goToPage" />
  </div>
</template>

<style scoped>
.subtitle {
  margin: 0 0 20px;
  color: #666;
  font-size: 14px;
}
.error {
  color: #a80000;
  font-size: 13px;
}
.loading {
  color: #666;
  font-size: 13px;
  margin: 0;
}
.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.list-toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.total-count {
  font-size: 12px;
  color: #999;
}
.refresh-button {
  font-size: 13px;
  padding: 5px 12px;
  cursor: pointer;
}
.refresh-button:disabled {
  cursor: default;
  opacity: 0.6;
}
</style>
