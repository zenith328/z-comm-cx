<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useBestReviewShortlist } from '../composables/useBestReviewShortlist'

const PAGE_SIZE = 10

const { entries, loading, generating, errorMessage, generate } = useBestReviewShortlist()

const collapsed = ref(false)
const visibleCount = ref(PAGE_SIZE)
const visibleEntries = computed(() => entries.value.slice(0, visibleCount.value))
const hasMore = computed(() => visibleCount.value < entries.value.length)

watch(entries, () => {
  visibleCount.value = PAGE_SIZE
})

function showMore() {
  visibleCount.value += PAGE_SIZE
}
</script>

<template>
  <section class="panel">
    <div class="panel-header">
      <button type="button" class="toggle-header" @click="collapsed = !collapsed">
        <span class="chevron" :class="{ collapsed }">▾</span>
        <span>
          <h2>베스트 리뷰 주간 숏리스트</h2>
          <p class="hint">
            매주 월요일 자동 생성되며(운영 환경 기준), 상품별 베스트 후보 리뷰 중 평점/최신순 상위 3개를 선정합니다.
          </p>
        </span>
      </button>
      <button type="button" :disabled="generating" @click="generate">
        {{ generating ? '생성 중...' : '이번 주 숏리스트 생성' }}
      </button>
    </div>

    <template v-if="!collapsed">
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-else-if="loading" class="loading">불러오는 중...</p>
      <p v-else-if="entries.length === 0" class="empty">
        아직 생성된 숏리스트가 없습니다. "이번 주 숏리스트 생성"을 눌러보세요.
      </p>

      <template v-else>
        <div class="shortlist-scroll">
          <table class="shortlist-table">
            <thead>
              <tr>
                <th>주차</th>
                <th>상품</th>
                <th>순위</th>
                <th>별점</th>
                <th class="content-col">내용</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="entry in visibleEntries" :key="`${entry.productCode}-${entry.rank}`">
                <td>{{ entry.weekLabel }}</td>
                <td>{{ entry.productCode }}</td>
                <td>{{ entry.rank }}</td>
                <td>{{ entry.review.rating }}</td>
                <td class="content-col">{{ entry.review.content }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <button v-if="hasMore" type="button" class="load-more-button" @click="showMore">
          더보기 ({{ entries.length - visibleCount }}건 더 있음)
        </button>
      </template>
    </template>
  </section>
</template>

<style scoped>
.panel {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 24px;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 12px;
}
.toggle-header {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  text-align: left;
  background: none;
  border: none;
  padding: 0;
  white-space: normal;
  cursor: pointer;
}
.chevron {
  font-size: 12px;
  color: #666;
  margin-top: 3px;
  transition: transform 0.15s ease;
}
.chevron.collapsed {
  transform: rotate(-90deg);
}
h2 {
  margin: 0 0 4px;
  font-size: 16px;
}
.hint {
  margin: 0;
  font-size: 12px;
  color: #777;
}
button {
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}
.shortlist-scroll {
  height: 200px;
  overflow-y: auto;
}
.shortlist-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.shortlist-table th,
.shortlist-table td {
  border-bottom: 1px solid #eee;
  padding: 6px 8px;
  text-align: left;
}
.shortlist-table th {
  position: sticky;
  top: 0;
  background: #fff;
}
.content-col {
  max-width: 400px;
}
.load-more-button {
  display: block;
  width: 100%;
  margin-top: 8px;
  padding: 6px 14px;
  font-size: 12px;
  color: #666;
  background: #fafafa;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}
.error {
  color: #a80000;
  font-size: 13px;
}
.loading,
.empty {
  color: #666;
  font-size: 13px;
}
</style>
