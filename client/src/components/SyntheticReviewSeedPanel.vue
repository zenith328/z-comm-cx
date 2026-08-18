<script setup lang="ts">
import { ref, watch } from 'vue'
import { fetchReviews, seedSyntheticReviews } from '../api/reviewApi'
import ClassificationBadge from './ClassificationBadge.vue'
import SentimentBadge from './SentimentBadge.vue'
import type { Review } from '../types/review'
import { isQuotaExceededError } from '../utils/apiError'

const props = defineProps<{
  productCode: string
  productName: string
  brand: string | null
  category: string | null
  description: string | null
}>()

const count = ref(5)
const seeding = ref(false)
const seedError = ref('')

const reviews = ref<Review[]>([])
const loadingList = ref(false)
const listError = ref('')

async function loadSyntheticReviews() {
  loadingList.value = true
  listError.value = ''
  try {
    const result = await fetchReviews({
      page: 0,
      size: 50,
      productCode: props.productCode,
      origin: 'SYNTHETIC',
    })
    reviews.value = result.content
  } catch (error) {
    console.error(error)
    listError.value = '생성된 테스트 리뷰를 불러오지 못했습니다.'
  } finally {
    loadingList.value = false
  }
}

async function seed() {
  seeding.value = true
  seedError.value = ''
  try {
    await seedSyntheticReviews({
      productCode: props.productCode,
      productName: props.productName,
      brand: props.brand,
      category: props.category,
      description: props.description,
      count: count.value,
    })
    await loadSyntheticReviews()
  } catch (error) {
    console.error(error)
    seedError.value = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과했습니다. 잠시 후 다시 시도해주세요.'
      : '테스트 리뷰 생성에 실패했습니다.'
  } finally {
    seeding.value = false
  }
}

function formatDate(value: string): string {
  return value.replace('T', ' ').slice(0, 16)
}

watch(() => props.productCode, loadSyntheticReviews, { immediate: true })
</script>

<template>
  <div class="synthetic-panel">
    <h4>AI 핏 가이드용 테스트 리뷰 생성</h4>
    <p class="hint">
      실제 고객 리뷰가 아니라, 핏 가이드 프롬프트 검증/시연용으로 AI가 다양한 체형의 가상 후기를 생성합니다.
    </p>
    <div class="synthetic-controls">
      <label>
        생성 개수
        <input v-model.number="count" type="number" min="1" max="20" />
      </label>
      <button type="button" :disabled="seeding" @click="seed">
        {{ seeding ? '생성 중...' : '테스트 리뷰 생성' }}
      </button>
    </div>
    <p v-if="seedError" class="error">{{ seedError }}</p>

    <h5 class="list-title">생성된 테스트 리뷰 ({{ reviews.length }}건)</h5>
    <p v-if="loadingList" class="loading">불러오는 중...</p>
    <p v-else-if="listError" class="error">{{ listError }}</p>
    <p v-else-if="reviews.length === 0" class="empty">아직 생성된 테스트 리뷰가 없습니다.</p>
    <ul v-else class="synthetic-review-list">
      <li v-for="review in reviews" :key="review.id" class="synthetic-review-item">
        <div class="synthetic-review-header">
          <span class="rating">★ {{ review.rating }}</span>
          <span class="author">{{ review.memberId }}</span>
          <ClassificationBadge :classification="review.classification" />
          <SentimentBadge :sentiment="review.sentiment" />
          <span class="date">{{ formatDate(review.createdAt) }}</span>
        </div>
        <p class="content">{{ review.content }}</p>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.synthetic-panel {
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 6px;
  background: #fafafa;
}
.synthetic-panel h4 {
  margin: 0 0 6px;
  font-size: 14px;
}
.hint {
  margin: 0 0 10px;
  font-size: 12px;
  color: #777;
}
.synthetic-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}
.synthetic-controls label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.synthetic-controls input {
  width: 60px;
  padding: 6px 8px;
  font-size: 13px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.synthetic-controls button {
  padding: 8px 14px;
  border: 1px solid #0056b3;
  border-radius: 4px;
  background: #0056b3;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}
.synthetic-controls button:disabled {
  background: #ccc;
  border-color: #ccc;
  cursor: default;
}
.error {
  margin: 8px 0 0;
  font-size: 12px;
  color: #a80000;
}
.list-title {
  margin: 16px 0 8px;
  font-size: 13px;
  color: #555;
}
.loading,
.empty {
  font-size: 12px;
  color: #888;
  margin: 0;
}
.synthetic-review-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.synthetic-review-item {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 8px;
}
.synthetic-review-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
}
.rating {
  color: #d68a00;
  font-weight: 600;
}
.author {
  color: #555;
}
.date {
  color: #999;
  margin-left: auto;
}
.content {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: #333;
}
</style>
