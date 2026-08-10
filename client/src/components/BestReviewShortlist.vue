<script setup lang="ts">
import { ref, watch } from 'vue'
import { fetchBestReviewShortlist } from '../api/clientReviewApi'
import type { ClientBestReviewShortlistEntry } from '../types/review'

const props = defineProps<{ productCode: string }>()

const entries = ref<ClientBestReviewShortlistEntry[]>([])
const open = ref(false)

async function load() {
  open.value = false
  try {
    entries.value = await fetchBestReviewShortlist(props.productCode)
  } catch (error) {
    console.error(error)
    entries.value = []
  }
}

function formatDate(value: string): string {
  return value.replace('T', ' ').slice(0, 10)
}

watch(() => props.productCode, load, { immediate: true })
</script>

<template>
  <section v-if="entries.length > 0" class="best-review-box">
    <button type="button" class="best-review-toggle" @click="open = !open">
      <h2>이번 주 베스트 리뷰</h2>
      <span class="toggle-icon" :class="{ open }">▾</span>
    </button>
    <template v-if="open">
      <p class="hint">{{ entries[0].weekLabel }} 주차 베스트 후보 리뷰 중 평점/최신순 상위 {{ entries.length }}개입니다.</p>
      <ul class="best-review-list">
        <li v-for="entry in entries" :key="entry.rank" class="best-review-item">
          <div class="best-review-item-header">
            <span class="rank">BEST {{ entry.rank }}</span>
            <span class="rating">★ {{ entry.review.rating }}</span>
            <span class="author">{{ entry.review.memberId }}</span>
            <span class="date">{{ formatDate(entry.review.createdAt) }}</span>
          </div>
          <p class="content">{{ entry.review.content }}</p>
        </li>
      </ul>
    </template>
  </section>
</template>

<style scoped>
.best-review-box {
  border: 1px solid #e0c987;
  background: #fffbf0;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 24px;
}
.best-review-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
}
.best-review-box h2 {
  margin: 0;
  font-size: 16px;
}
.toggle-icon {
  color: #b8860b;
  font-size: 14px;
  transition: transform 0.15s ease;
}
.toggle-icon.open {
  transform: rotate(180deg);
}
.hint {
  margin: 8px 0 12px;
  font-size: 12px;
  color: #777;
}
.best-review-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.best-review-item {
  padding: 10px 0;
  border-top: 1px solid #eee;
}
.best-review-item:first-child {
  border-top: none;
}
.best-review-item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  font-size: 13px;
}
.rank {
  color: #b8860b;
  font-weight: 700;
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
  font-size: 12px;
  margin-left: auto;
}
.content {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
}
</style>
