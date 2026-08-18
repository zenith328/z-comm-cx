<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductReviewSection from '../components/ProductReviewSection.vue'
import { fetchProduct } from '../api/products'
import { fetchResolvedDescription } from '../api/productDescriptionVariants'
import { setBreadcrumbProductName } from '../stores/breadcrumb'
import { session } from '../stores/session'
import type { Product } from '../types/product'

const route = useRoute()
const router = useRouter()

const product = ref<Product | null>(null)
const quantity = ref(1)
const loading = ref(false)
const errorMessage = ref('')

const description = ref<string | null>(null)
const descriptionPersonalized = ref(false)

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
    await loadDescription()
  } catch {
    errorMessage.value = '상품 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function loadDescription() {
  try {
    const resolved = await fetchResolvedDescription(
      productId.value,
      session.current?.gender ?? null,
      session.current?.age ?? null,
    )
    description.value = resolved.text
    descriptionPersonalized.value = resolved.personalized
  } catch (error) {
    console.error(error)
    description.value = null
    descriptionPersonalized.value = false
  }
}

// 헤더의 "내 정보"에서 성별/연령을 바꾸면 session.current가 갱신되는데, 이 화면을 보고 있는 동안이면
// 새로고침 없이도 그 즉시 알맞은(또는 기본) 설명으로 다시 조회되도록 감시한다.
watch(
  () => [session.current?.gender, session.current?.age],
  () => {
    if (product.value) loadDescription()
  },
)

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
          <p v-if="outOfStock" class="stock-out">품절</p>

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

      <section v-if="description" class="product-description">
        <h3>
          상품 상세설명
          <span v-if="descriptionPersonalized" class="personalized-badge">고객님을 위한 맞춤 설명</span>
        </h3>
        <p class="description-text">{{ description }}</p>
      </section>

      <ProductReviewSection
        :product-code="product.productCode"
        :category="product.category"
        @write-review="goWriteReview"
      />
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
.stock-out {
  display: inline-block;
  width: fit-content;
  padding: 3px 10px;
  border-radius: 4px;
  background: #fde2e2;
  color: #a80000;
  font-size: 13px;
  font-weight: 600;
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
.product-description {
  margin-bottom: 24px;
  padding: 16px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 8px;
}
.product-description h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 15px;
}
.personalized-badge {
  padding: 2px 8px;
  border-radius: 999px;
  background: #e6f0fb;
  color: #0056b3;
  font-size: 11px;
  font-weight: 600;
}
.description-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  white-space: pre-wrap;
}
</style>
