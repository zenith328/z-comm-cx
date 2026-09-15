<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchProduct } from '../api/products'
import { setBreadcrumbProductName } from '../stores/breadcrumb'
import ShowhostBroadcastPanel from '../components/ShowhostBroadcastPanel.vue'
import type { Product } from '../types/product'

const route = useRoute()
const productId = computed(() => Number(route.params.id))

const product = ref<Product | null>(null)
const loading = ref(false)
const loadError = ref('')

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    product.value = await fetchProduct(productId.value)
    setBreadcrumbProductName(product.value.name)
  } catch (error) {
    console.error(error)
    loadError.value = '상품 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
onUnmounted(() => setBreadcrumbProductName(null))
</script>

<template>
  <div class="showhost-view">
    <p v-if="loading" class="loading">방송을 준비하는 중...</p>
    <p v-else-if="loadError" class="error">{{ loadError }}</p>

    <template v-else-if="product">
      <header class="product-summary">
        <p v-if="product.brand" class="brand">{{ product.brand }}</p>
        <h2>{{ product.name }}</h2>
      </header>

      <ShowhostBroadcastPanel :product-code="product.productCode" />
    </template>
  </div>
</template>

<style scoped>
.loading,
.error {
  font-size: 13px;
}
.error {
  color: #a80000;
}
.product-summary {
  margin-bottom: 16px;
}
.brand {
  margin: 0 0 4px;
  font-size: 12px;
  color: #888;
}
.product-summary h2 {
  margin: 0;
  font-size: 20px;
}
</style>
