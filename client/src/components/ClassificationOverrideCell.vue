<script setup lang="ts">
import { ref } from 'vue'
import { CLASSIFICATION_LABELS, CLASSIFICATION_OPTIONS } from '../constants/review'
import type { Review, ReviewOverrideRequest } from '../types/review'
import ClassificationBadge from './ClassificationBadge.vue'
import VisibilityBadge from './VisibilityBadge.vue'

const props = defineProps<{ review: Review; saving: boolean }>()
const emit = defineEmits<{ override: [request: ReviewOverrideRequest] }>()

const editing = ref(false)
const selectedVisible = ref(props.review.visible)
const selectedClassification = ref(props.review.classification)
const note = ref('')

function startEdit() {
  selectedVisible.value = props.review.visible
  selectedClassification.value = props.review.classification
  note.value = ''
  editing.value = true
}

function cancel() {
  editing.value = false
}

function confirm() {
  emit('override', {
    visible: selectedVisible.value,
    classification: selectedClassification.value,
    note: note.value.trim() || null,
  })
  editing.value = false
}

function overriddenTitle(review: Review): string {
  if (review.classificationSource !== 'ADMIN') return ''
  const at = review.overriddenAt ? review.overriddenAt.replace('T', ' ').slice(0, 19) : ''
  const noteSuffix = review.overrideNote ? ` - ${review.overrideNote}` : ''
  return `관리자 수동 조정 (${at})${noteSuffix}`
}
</script>

<template>
  <div class="cell">
    <template v-if="!editing">
      <VisibilityBadge :visible="review.visible" />
      <ClassificationBadge :classification="review.classification" />
      <span v-if="review.classificationSource === 'ADMIN'" class="manual-tag" :title="overriddenTitle(review)">
        관리자 조정
      </span>
      <button type="button" class="link-button" :disabled="saving" @click="startEdit">조정</button>
    </template>
    <template v-else>
      <select v-model="selectedVisible">
        <option :value="true">공개</option>
        <option :value="false">비공개</option>
      </select>
      <select v-model="selectedClassification">
        <option v-for="option in CLASSIFICATION_OPTIONS" :key="option" :value="option">
          {{ CLASSIFICATION_LABELS[option] }}
        </option>
      </select>
      <input v-model="note" class="note-input" placeholder="변경 사유(선택)" />
      <button type="button" :disabled="saving" @click="confirm">저장</button>
      <button type="button" :disabled="saving" @click="cancel">취소</button>
    </template>
  </div>
</template>

<style scoped>
.cell {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.manual-tag {
  font-size: 11px;
  color: #a15c00;
  border: 1px solid #a15c00;
  border-radius: 4px;
  padding: 0 4px;
  cursor: help;
}
.link-button {
  border: none;
  background: none;
  color: #0056b3;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
}
select,
.note-input {
  font-size: 12px;
  padding: 2px 4px;
}
.note-input {
  width: 120px;
}
button:not(.link-button) {
  font-size: 12px;
  padding: 2px 8px;
  cursor: pointer;
}
</style>
