<script setup lang="ts">
import { CLASSIFICATION_LABELS, CLASSIFICATION_OPTIONS, STATUS_LABELS } from '../constants/review'
import type { ReviewClassification, ReviewStatus } from '../types/review'

const productCode = defineModel<string>('productCode', { required: true })
const visible = defineModel<boolean | 'ALL'>('visible', { required: true })
const classification = defineModel<ReviewClassification | 'ALL'>('classification', { required: true })
const status = defineModel<ReviewStatus | 'ALL'>('status', { required: true })

const STATUS_OPTIONS = Object.keys(STATUS_LABELS) as ReviewStatus[]
</script>

<template>
  <div class="filter-bar">
    <label>
      상품코드
      <input v-model="productCode" type="text" placeholder="상품코드로 검색" class="product-code-input" />
    </label>
    <label>
      공개여부
      <select v-model="visible">
        <option value="ALL">전체</option>
        <option :value="true">공개</option>
        <option :value="false">비공개</option>
      </select>
    </label>
    <label>
      분류
      <select v-model="classification">
        <option value="ALL">전체</option>
        <option v-for="option in CLASSIFICATION_OPTIONS" :key="option" :value="option">
          {{ CLASSIFICATION_LABELS[option] }}
        </option>
      </select>
    </label>
    <label>
      상태
      <select v-model="status">
        <option value="ALL">전체</option>
        <option v-for="option in STATUS_OPTIONS" :key="option" :value="option">
          {{ STATUS_LABELS[option] }}
        </option>
      </select>
    </label>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
select {
  font-size: 13px;
  padding: 3px 6px;
}
.product-code-input {
  font-size: 13px;
  padding: 3px 6px;
  width: 140px;
}
</style>
