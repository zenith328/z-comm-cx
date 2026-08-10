<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import FoBrandFilterBar from '../components/FoBrandFilterBar.vue'
import FoProductCard from '../components/FoProductCard.vue'
import type { Product } from '../types/product'

const PAGE_SIZE = 25

type SortOption = 'latest' | 'reviewCount' | 'name'

const props = defineProps<{
  products: Product[]
  loading: boolean
  errorMessage: string
  brands: string[]
  selectedBrand: string | null
}>()
const emit = defineEmits<{ select: [product: Product]; brandSelect: [brand: string | null] }>()

const page = ref(1)
const sortOption = ref<SortOption>('latest')

const sortedProducts = computed(() => {
  if (sortOption.value === 'reviewCount') {
    return [...props.products].sort((a, b) => b.reviewCount - a.reviewCount)
  }
  if (sortOption.value === 'name') {
    return [...props.products].sort((a, b) => a.name.localeCompare(b.name, 'ko'))
  }
  // 서버가 이미 최신순(createdAt desc)으로 내려주므로 그대로 사용
  return props.products
})

const totalPages = computed(() => Math.max(1, Math.ceil(sortedProducts.value.length / PAGE_SIZE)))

const pagedProducts = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return sortedProducts.value.slice(start, start + PAGE_SIZE)
})

watch(
  () => props.products.length,
  () => {
    if (page.value > totalPages.value) page.value = totalPages.value
  },
)

watch(
  () => props.selectedBrand,
  () => {
    page.value = 1
  },
)

watch(sortOption, () => {
  page.value = 1
})

function goPrev() {
  if (page.value > 1) page.value -= 1
}

function goNext() {
  if (page.value < totalPages.value) page.value += 1
}
</script>

<template>
  <div>
    <div class="toolbar">
      <FoBrandFilterBar
        :brands="brands"
        :selected="selectedBrand"
        @select="(brand) => emit('brandSelect', brand)"
      />
      <select v-model="sortOption" class="sort-select">
        <option value="latest">최신순</option>
        <option value="reviewCount">리뷰많은순</option>
        <option value="name">상품명순</option>
      </select>
    </div>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-else-if="loading && products.length === 0" class="loading">불러오는 중...</p>
    <p v-else-if="!loading && products.length === 0" class="empty">등록된 상품이 없습니다.</p>

    <template v-else>
      <div class="product-grid" :class="{ 'is-loading': loading }">
        <FoProductCard
          v-for="product in pagedProducts"
          :key="product.id"
          :product="product"
          @select="(p) => emit('select', p)"
        />
      </div>

      <nav class="pagination">
        <button type="button" :disabled="page === 1" @click="goPrev">&lt;</button>
        <span class="page-number">{{ page }} / {{ totalPages }}</span>
        <button type="button" :disabled="page === totalPages" @click="goNext">&gt;</button>
      </nav>
    </template>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.sort-select {
  font-size: 13px;
  padding: 3px 6px;
  flex-shrink: 0;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 20px;
  transition: opacity 0.15s ease;
}
.product-grid.is-loading {
  opacity: 0.5;
  pointer-events: none;
}
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}
.pagination button {
  padding: 4px 12px;
  font-size: 14px;
  cursor: pointer;
}
.pagination button:disabled {
  cursor: default;
  opacity: 0.4;
}
.page-number {
  font-size: 13px;
  color: #333;
  min-width: 48px;
  text-align: center;
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
