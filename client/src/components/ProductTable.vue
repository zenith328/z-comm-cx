<script setup lang="ts">
import { ref } from 'vue'
import type { Product } from '../types/product'

defineProps<{ products: Product[] }>()
const emit = defineEmits<{ restock: [id: number, quantity: number]; outOfStock: [id: number] }>()

const restockingId = ref<number | null>(null)
const restockDraft = ref(10)

function startRestock(product: Product) {
  restockingId.value = product.id
  restockDraft.value = 10
}

function submitRestock(product: Product) {
  if (restockDraft.value <= 0) return
  emit('restock', product.id, restockDraft.value)
  restockingId.value = null
}

function formatPrice(price: number | null): string {
  return price == null ? '-' : `${price.toLocaleString()}원`
}

function formatDate(value: string): string {
  return value.replace('T', ' ').slice(0, 19)
}
</script>

<template>
  <table class="product-table">
    <thead>
      <tr>
        <th>이미지</th>
        <th>상품코드</th>
        <th>상품명</th>
        <th>브랜드</th>
        <th>가격</th>
        <th>리뷰</th>
        <th>재고</th>
        <th>주문가능</th>
        <th>등록일시</th>
        <th>액션</th>
      </tr>
    </thead>
    <tbody>
      <tr v-if="products.length === 0">
        <td colspan="10" class="empty">등록된 상품이 없습니다.</td>
      </tr>
      <tr v-for="product in products" :key="product.id">
        <td>
          <img v-if="product.imageUrls[0]" :src="product.imageUrls[0]" class="thumb" alt="" />
        </td>
        <td>{{ product.productCode }}</td>
        <td>
          <a :href="product.sourceUrl" target="_blank" rel="noopener noreferrer">{{ product.name }}</a>
        </td>
        <td>{{ product.brand ?? '-' }}</td>
        <td>{{ formatPrice(product.price) }}</td>
        <td>{{ product.reviewCount }}건</td>
        <td>{{ product.stockQuantity ?? '-' }}</td>
        <td>{{ product.availableQuantity ?? '-' }}</td>
        <td>{{ formatDate(product.createdAt) }}</td>
        <td>
          <div v-if="restockingId === product.id" class="restock-box">
            <input v-model.number="restockDraft" type="number" min="1" class="quantity-input" />
            <div class="restock-buttons">
              <button type="button" @click="submitRestock(product)">확인</button>
              <button type="button" @click="restockingId = null">취소</button>
            </div>
          </div>
          <div v-else class="stock-actions">
            <button type="button" @click="startRestock(product)">입고</button>
            <button type="button" @click="emit('outOfStock', product.id)">품절</button>
            <RouterLink :to="`/admin/products/${product.id}`" class="detail-link">상품상세 관리</RouterLink>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
.product-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
th,
td {
  border-bottom: 1px solid #eee;
  padding: 8px 10px;
  text-align: left;
  vertical-align: middle;
}
th {
  background: #f5f5f5;
  font-weight: 600;
  white-space: nowrap;
}
.thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
  display: block;
}
.empty {
  text-align: center;
  color: #999;
  padding: 24px;
}
.stock-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}
.detail-link {
  padding: 4px 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: #fff;
  color: #333;
  font-size: 12px;
  text-decoration: none;
  white-space: nowrap;
}
.detail-link:hover {
  border-color: #0056b3;
  color: #0056b3;
}
.restock-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.restock-buttons {
  display: flex;
  gap: 4px;
}
.quantity-input {
  width: 90px;
  box-sizing: border-box;
}
</style>
