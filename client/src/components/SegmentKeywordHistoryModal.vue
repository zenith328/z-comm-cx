<script setup lang="ts">
import { ref, watch } from 'vue'
import { fetchSegmentKeywordHistory } from '../api/segmentKeywords'
import type { CustomerSegment, SegmentKeywordHistoryResponse } from '../api/cs-types'

const props = defineProps<{ open: boolean; segment: CustomerSegment | null; segmentLabel: string }>()
const emit = defineEmits<{ close: [] }>()

const items = ref<SegmentKeywordHistoryResponse[]>([])
const loading = ref(false)
const error = ref('')

// 열릴 때마다(세그먼트가 바뀌어도) 최신 이력을 새로 불러온다.
watch(
  () => [props.open, props.segment],
  async () => {
    if (!props.open || !props.segment) return
    loading.value = true
    error.value = ''
    items.value = []
    try {
      items.value = await fetchSegmentKeywordHistory(props.segment)
    } catch (e) {
      console.error(e)
      error.value = '이력을 불러오지 못했습니다.'
    } finally {
      loading.value = false
    }
  },
  { immediate: true },
)

function formatChangedAt(value: string): string {
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <div v-if="open" class="overlay" @click.self="emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>{{ segmentLabel }} 키워드 변경 이력</h3>
        <button type="button" class="close-button" @click="emit('close')">×</button>
      </div>
      <div class="modal-body">
        <p v-if="loading" class="loading">불러오는 중...</p>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="!loading && !error && items.length === 0" class="empty">저장된 이력이 없습니다.</p>
        <ul v-if="!loading && !error && items.length > 0" class="history-list">
          <li v-for="(item, index) in items" :key="index" class="history-item">
            <span class="history-date">{{ formatChangedAt(item.changedAt) }}</span>
            <span class="history-keywords">{{ item.keywords || '(빈 값)' }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.modal {
  width: 480px;
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 64px);
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.modal-header h3 {
  margin: 0;
  font-size: 16px;
}
.close-button {
  border: none;
  background: none;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  color: #666;
  padding: 0;
}
.modal-body {
  padding: 16px 20px 20px;
  overflow-y: auto;
}
.loading,
.empty {
  margin: 0;
  font-size: 13px;
  color: #888;
}
.error {
  margin: 0;
  font-size: 13px;
  color: #a80000;
}
.history-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.history-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 8px 10px;
  border: 1px solid #eee;
  border-radius: 6px;
  background: #fafafa;
}
.history-date {
  font-size: 11px;
  color: #999;
}
.history-keywords {
  font-size: 13px;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
