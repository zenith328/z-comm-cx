<script setup lang="ts">
import type { Review, ReviewOverrideRequest } from '../types/review'
import ClassificationOverrideCell from './ClassificationOverrideCell.vue'
import ReviewTypeBadge from './ReviewTypeBadge.vue'
import SentimentBadge from './SentimentBadge.vue'
import StatusBadge from './StatusBadge.vue'

defineProps<{ reviews: Review[]; overridingId: number | null; reanalyzingId: number | null }>()
const emit = defineEmits<{
  override: [id: number, request: ReviewOverrideRequest]
  reanalyze: [id: number]
}>()

function formatDate(value: string): string {
  return value.replace('T', ' ').slice(0, 19)
}
</script>

<template>
  <div class="table-wrapper">
    <table class="review-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>상품</th>
          <th>회원</th>
          <th>별점</th>
          <th>리뷰타입</th>
          <th class="content-col">내용</th>
          <th>등록일시</th>
          <th>상태</th>
          <th class="classification-col">공개여부/분류</th>
          <th>감성</th>
          <th>위험도</th>
          <th class="content-col">AI 판단 사유</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="reviews.length === 0">
          <td colspan="12" class="empty">등록된 리뷰가 없습니다.</td>
        </tr>
        <tr v-for="review in reviews" :key="review.id">
          <td>{{ review.id }}</td>
          <td>{{ review.productCode }}</td>
          <td>{{ review.memberId }}</td>
          <td>{{ review.rating }}</td>
          <td><ReviewTypeBadge :has-photo="review.hasPhoto" /></td>
          <td class="content-col">{{ review.content }}</td>
          <td>{{ formatDate(review.createdAt) }}</td>
          <td>
            <StatusBadge :status="review.status" />
            <button
              v-if="review.status === 'FAILED'"
              type="button"
              class="reanalyze-button"
              :disabled="reanalyzingId === review.id"
              @click="emit('reanalyze', review.id)"
            >
              {{ reanalyzingId === review.id ? '재시도 중...' : '재시도' }}
            </button>
          </td>
          <td class="classification-col">
            <ClassificationOverrideCell
              :review="review"
              :saving="overridingId === review.id"
              @override="(request) => emit('override', review.id, request)"
            />
          </td>
          <td><SentimentBadge :sentiment="review.sentiment" /></td>
          <td>{{ review.riskScore ?? '-' }}</td>
          <td class="content-col">{{ review.aiReason ?? '-' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-wrapper {
  overflow-x: auto;
}
.review-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
th,
td {
  border-bottom: 1px solid #eee;
  padding: 8px 10px;
  text-align: left;
  vertical-align: top;
}
th {
  background: #f5f5f5;
  font-weight: 600;
  white-space: nowrap;
}
.content-col {
  max-width: 420px;
}
.classification-col {
  min-width: 200px;
}
.empty {
  text-align: center;
  color: #999;
  padding: 24px;
}
.reanalyze-button {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  padding: 2px 8px;
  cursor: pointer;
}
.reanalyze-button:disabled {
  cursor: default;
  opacity: 0.6;
}
</style>
