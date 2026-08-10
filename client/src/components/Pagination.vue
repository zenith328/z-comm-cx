<script setup lang="ts">
import { computed } from 'vue'

const DELTA = 1

const props = defineProps<{ page: number; totalPages: number; totalElements: number }>()
const emit = defineEmits<{ change: [page: number] }>()

type PageItem = number | 'ellipsis'

const pageItems = computed<PageItem[]>(() => {
  const total = Math.max(props.totalPages, 1)
  const current = props.page + 1

  if (total <= 1) return [1]

  const items: PageItem[] = [1]
  const rangeStart = Math.max(2, current - DELTA)
  const rangeEnd = Math.min(total - 1, current + DELTA)

  if (rangeStart > 2) items.push('ellipsis')
  for (let p = rangeStart; p <= rangeEnd; p++) items.push(p)
  if (rangeEnd < total - 1) items.push('ellipsis')
  items.push(total)

  return items
})

function goTo(target: number) {
  emit('change', target - 1)
}
</script>

<template>
  <nav class="pagination">
    <span class="total-count">총 {{ totalElements }}건</span>
    <div class="pagination-controls">
      <button type="button" :disabled="page === 0" @click="emit('change', page - 1)">&lt;</button>
      <template v-for="(item, index) in pageItems" :key="index">
        <span v-if="item === 'ellipsis'" class="ellipsis">...</span>
        <button
          v-else
          type="button"
          class="page-button"
          :class="{ active: item === page + 1 }"
          @click="goTo(item)"
        >
          {{ item }}
        </button>
      </template>
      <button type="button" :disabled="page >= totalPages - 1" @click="emit('change', page + 1)">&gt;</button>
    </div>
  </nav>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}
.pagination-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}
.pagination-controls button {
  padding: 4px 10px;
  font-size: 13px;
  cursor: pointer;
}
.pagination-controls button:disabled {
  cursor: default;
  opacity: 0.4;
}
.page-button.active {
  font-weight: 700;
  color: #0056b3;
  text-decoration: underline;
}
.ellipsis {
  padding: 0 4px;
  color: #999;
  font-size: 13px;
}
.total-count {
  font-size: 12px;
  color: #999;
}
</style>
