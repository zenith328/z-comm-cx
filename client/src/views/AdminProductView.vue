<script setup lang="ts">
import { ref, watch } from 'vue'
import Pagination from '../components/Pagination.vue'
import ProductRegisterForm from '../components/ProductRegisterForm.vue'
import ProductTable from '../components/ProductTable.vue'
import { useProducts } from '../composables/useProducts'

const {
  products,
  loading,
  errorMessage,
  page,
  totalPages,
  totalElements,
  registerProduct,
  setProductCodeFilter,
  goToPage,
  restock,
  markOutOfStock,
} = useProducts()

const registering = ref(false)
const registerError = ref('')
const productCodeInput = ref('')

let debounceTimer: ReturnType<typeof setTimeout> | undefined
watch(productCodeInput, (value) => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => setProductCodeFilter(value), 300)
})

async function handleRegister(url: string) {
  registering.value = true
  registerError.value = ''
  try {
    await registerProduct(url)
  } catch (error) {
    console.error(error)
    registerError.value = '상품 등록에 실패했습니다. URL을 확인해주세요.'
  } finally {
    registering.value = false
  }
}
</script>

<template>
  <div>
    <p class="subtitle">
      외부 쇼핑몰 상품 URL로 상품을 등록하면, 재고가 0으로 생성되고 리뷰 작성·주문·재고관리를 이 화면에서 모두 처리할 수 있습니다.
    </p>

    <ProductRegisterForm :registering="registering" @register="handleRegister" />
    <p v-if="registerError" class="error">{{ registerError }}</p>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-if="loading" class="loading">불러오는 중...</p>

    <div class="filter-bar">
      <label>
        상품코드
        <input v-model="productCodeInput" type="text" placeholder="상품코드로 검색" class="product-code-input" />
      </label>
    </div>
    <ProductTable :products="products" @restock="restock" @out-of-stock="markOutOfStock" />

    <Pagination :page="page" :total-pages="totalPages" :total-elements="totalElements" @change="goToPage" />
  </div>
</template>

<style scoped>
.subtitle {
  margin: 0 0 20px;
  color: #666;
  font-size: 14px;
}
.error {
  color: #a80000;
  font-size: 13px;
}
.loading {
  color: #666;
  font-size: 13px;
}
.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}
.filter-bar label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.product-code-input {
  font-size: 13px;
  padding: 3px 6px;
  width: 140px;
}
</style>
