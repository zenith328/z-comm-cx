<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductReviewSection from '../components/ProductReviewSection.vue'
import { fetchProduct } from '../api/products'
import { setBreadcrumbProductName } from '../stores/breadcrumb'
import type { Product } from '../types/product'

const route = useRoute()
const router = useRouter()

const product = ref<Product | null>(null)
const quantity = ref(1)
const loading = ref(false)
const errorMessage = ref('')

const productId = computed(() => Number(route.params.id))
const outOfStock = computed(() =>
  product.value?.availableQuantity != null ? product.value.availableQuantity <= 0 : false,
)

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    product.value = await fetchProduct(productId.value)
    quantity.value = 1
    setBreadcrumbProductName(product.value.name)
  } catch {
    errorMessage.value = '상품 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function goOrder() {
  router.push({ path: '/orders/new', query: { productId: productId.value, quantity: quantity.value } })
}

function goWriteReview() {
  router.push(`/products/${productId.value}/review`)
}

function formatPrice(price: number | null): string {
  return price == null ? '-' : `${price.toLocaleString()}원`
}

function formatDate(value: string): string {
  return value.slice(0, 10)
}

watch(productId, load)
onMounted(load)
onUnmounted(() => setBreadcrumbProductName(null))
</script>

<template>
  <div>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-else-if="loading">불러오는 중...</p>

    <template v-else-if="product">
      <section class="product-summary">
        <div class="thumb-wrap">
          <img v-if="product.imageUrls[0]" :src="product.imageUrls[0]" class="thumb" alt="" />
        </div>
        <div class="info">
          <p v-if="product.brand" class="brand">{{ product.brand }}</p>
          <p class="product-code">{{ product.productCode }}</p>
          <h2>{{ product.name }}</h2>
          <p class="price">{{ formatPrice(product.price) }}</p>
          <p class="registered-at">등록일 {{ formatDate(product.createdAt) }}</p>
          <p v-if="product.stockQuantity != null" class="stock">
            <span>재고 {{ product.stockQuantity }}개</span>
            <span>주문가능 {{ outOfStock ? '품절' : `${product.availableQuantity}개` }}</span>
          </p>

          <div class="actions">
            <div class="order-box">
              <label>
                수량
                <input
                  v-model.number="quantity"
                  type="number"
                  min="1"
                  :max="product.availableQuantity ?? 1"
                  :disabled="outOfStock"
                />
              </label>
              <button type="button" :disabled="outOfStock || quantity < 1" @click="goOrder">주문하기</button>
            </div>
          </div>
        </div>
      </section>

      <ProductReviewSection :product-code="product.productCode" @write-review="goWriteReview" />
    </template>
  </div>
</template>

<style scoped>
.error {
  color: #a80000;
  font-size: 13px;
}
.product-summary {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}
.thumb-wrap {
  width: 200px;
  height: 200px;
  flex-shrink: 0;
  background: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
}
.thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.info {
  display: flex;
  flex-direction: column;
}
.brand {
  margin: 0 0 4px;
  font-size: 13px;
  color: #666;
}
.product-code {
  margin: 0 0 4px;
  font-size: 12px;
  color: #999;
}
.info h2 {
  margin: 0 0 8px;
  font-size: 18px;
}
.price {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 700;
}
.registered-at {
  margin: 0 0 12px;
  font-size: 12px;
  color: #999;
}
.stock {
  display: flex;
  gap: 16px;
  color: #555;
  font-size: 13px;
  margin: 0 0 16px;
}
.actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: auto;
}
.order-box {
  display: flex;
  align-items: center;
  gap: 10px;
}
.order-box label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.order-box input {
  width: 64px;
  padding: 6px 8px;
  border: 1px solid #ccc;
  border-radius: 6px;
}
.order-box button {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  background: #0056b3;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.order-box button:disabled {
  background: #a7c4e0;
  cursor: not-allowed;
}
</style>
