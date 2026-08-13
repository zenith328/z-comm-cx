<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchProduct } from '../api/products'
import ProductDescriptionVariantPanel from '../components/ProductDescriptionVariantPanel.vue'
import type { Product } from '../types/product'

const route = useRoute()
const productId = Number(route.params.id)

const product = ref<Product | null>(null)
const loading = ref(false)
const loadError = ref('')

function formatPrice(price: number | null): string {
  return price == null ? '-' : `${price.toLocaleString()}원`
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    product.value = await fetchProduct(productId)
  } catch (error) {
    console.error(error)
    loadError.value = '상품 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <RouterLink to="/admin/products" class="back-link">← 상품관리로 돌아가기</RouterLink>

    <p v-if="loading" class="loading">불러오는 중...</p>
    <p v-if="loadError" class="error">{{ loadError }}</p>

    <template v-if="product">
      <div class="product-header">
        <img v-if="product.imageUrls[0]" :src="product.imageUrls[0]" class="thumb" alt="" />
        <div class="product-header-info">
          <h2>{{ product.name }}</h2>
          <dl>
            <dt>상품코드</dt>
            <dd>{{ product.productCode }}</dd>
            <dt>브랜드</dt>
            <dd>{{ product.brand ?? '-' }}</dd>
            <dt>가격</dt>
            <dd>{{ formatPrice(product.price) }}</dd>
          </dl>
        </div>
      </div>

      <ProductDescriptionVariantPanel :product-id="productId" />
    </template>
  </div>
</template>

<style scoped>
.back-link {
  display: inline-block;
  margin-bottom: 16px;
  font-size: 13px;
  color: #0056b3;
  text-decoration: none;
}
.back-link:hover {
  text-decoration: underline;
}
.loading,
.error {
  font-size: 13px;
}
.error {
  color: #a80000;
}
.product-header {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}
.product-header .thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}
.product-header-info h2 {
  margin: 0 0 8px;
  font-size: 18px;
}
.product-header-info dl {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 2px 12px;
  margin: 0;
  font-size: 13px;
}
.product-header-info dt {
  color: #888;
}
.product-header-info dd {
  margin: 0;
  color: #333;
}
</style>
