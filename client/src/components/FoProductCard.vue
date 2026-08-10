<script setup lang="ts">
import type { Product } from '../types/product'

defineProps<{ product: Product }>()
const emit = defineEmits<{ select: [product: Product] }>()

function formatPrice(price: number | null): string {
  return price == null ? '-' : `${price.toLocaleString()}원`
}
</script>

<template>
  <button type="button" class="card" @click="emit('select', product)">
    <div class="thumb-wrap">
      <img v-if="product.imageUrls[0]" :src="product.imageUrls[0]" class="thumb" alt="" />
    </div>
    <p v-if="product.brand" class="brand">{{ product.brand }}</p>
    <p class="product-code">{{ product.productCode }}</p>
    <p class="name">{{ product.name }}</p>
    <p class="price">{{ formatPrice(product.price) }}</p>
    <p class="review-count">리뷰 {{ product.reviewCount }}건</p>
  </button>
</template>

<style scoped>
.card {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  text-align: left;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 10px;
  background: #fff;
  cursor: pointer;
  font: inherit;
}
.card:hover {
  border-color: #0056b3;
}
.thumb-wrap {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f5f5f5;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 8px;
}
.thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.brand {
  margin: 0 0 2px;
  font-size: 12px;
  color: #666;
}
.product-code {
  margin: 0 0 2px;
  font-size: 11px;
  color: #999;
}
.name {
  margin: 0 0 4px;
  font-size: 13px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.8em;
}
.price {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
}
.review-count {
  margin: 0;
  font-size: 12px;
  color: #888;
}
</style>
