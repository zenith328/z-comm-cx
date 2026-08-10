<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createOrder } from '../api/orders'
import { fetchProduct } from '../api/products'
import type { Product } from '../types/product'
import {
  getShippingHistory,
  saveShippingHistory,
  type ShippingHistoryEntry,
} from '../utils/orderHistory'
import { session } from '../stores/session'

const route = useRoute()
const router = useRouter()

const productId = computed(() => Number(route.query.productId))
const quantity = computed(() => Number(route.query.quantity) || 1)

const product = ref<Product | null>(null)
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

const form = reactive({
  customerName: '',
  customerPhone: '',
  recipientName: '',
  recipientPhone: '',
  zipcode: '',
  address1: '',
  address2: '',
})

const SHIPPING_PRESETS = [
  {
    label: '홍길동 / 서울시 강남구',
    recipientName: '홍길동',
    recipientPhone: '010-1111-2222',
    zipcode: '12345',
    address1: '서울시 강남구',
    address2: '101동 101호',
  },
  {
    label: '김철수 / 인천시 남동구',
    recipientName: '김철수',
    recipientPhone: '010-2222-3333',
    zipcode: '11111',
    address1: '인천시 남동구',
    address2: '',
  },
  {
    label: '회사(총무팀) / 서울시 서초구',
    recipientName: '총무팀',
    recipientPhone: '010-9999-8888',
    zipcode: '06000',
    address1: '서울시 서초구',
    address2: '3층 총무팀',
  },
]

const shippingHistory = ref<ShippingHistoryEntry[]>([])

const shippingPresetIndex = ref('')

function applyShippingPreset() {
  const [type, indexText] = shippingPresetIndex.value.split(':')
  const index = Number(indexText)
  const preset = type === 'history' ? shippingHistory.value[index] : SHIPPING_PRESETS[index]
  if (!preset) return
  form.recipientName = preset.recipientName
  form.recipientPhone = preset.recipientPhone
  form.zipcode = preset.zipcode
  form.address1 = preset.address1
  form.address2 = preset.address2
}

const totalPrice = computed(() => (product.value ? (product.value.price ?? 0) * quantity.value : 0))

async function load() {
  shippingHistory.value = getShippingHistory()

  if (session.current) {
    form.customerName = session.current.name
    form.customerPhone = session.current.phone
  }

  if (!productId.value) {
    errorMessage.value = '주문할 상품 정보가 없습니다. 상품 상세 화면에서 다시 시도해 주세요.'
    return
  }
  loading.value = true
  try {
    product.value = await fetchProduct(productId.value)
  } catch {
    errorMessage.value = '상품 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!product.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await createOrder({
      customerName: form.customerName,
      customerPhone: form.customerPhone,
      recipientName: form.recipientName,
      recipientPhone: form.recipientPhone,
      zipcode: form.zipcode,
      address1: form.address1,
      address2: form.address2,
      items: [{ productId: productId.value, quantity: quantity.value }],
    })
    saveShippingHistory({
      recipientName: form.recipientName,
      recipientPhone: form.recipientPhone,
      zipcode: form.zipcode,
      address1: form.address1,
      address2: form.address2,
    })
    router.push('/orders')
  } catch {
    errorMessage.value = '주문 처리 중 오류가 발생했습니다. 입력값을 확인해 주세요.'
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="checkout">
    <RouterLink v-if="productId" :to="`/products/${productId}`">&larr; 상품으로 돌아가기</RouterLink>

    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="errorMessage && !product" class="error">{{ errorMessage }}</p>

    <template v-else-if="product">
      <h2>주문하기</h2>

      <div class="summary">
        <span>{{ product.name }}</span>
        <span>{{ quantity }}개</span>
        <span class="total">{{ totalPrice.toLocaleString() }}원</span>
      </div>

      <form class="order-form" @submit.prevent="submit">
        <fieldset>
          <legend>주문자 정보</legend>
          <label>이름<input v-model="form.customerName" readonly /></label>
          <label>연락처<input v-model="form.customerPhone" readonly /></label>
        </fieldset>

        <fieldset>
          <legend>배송지 정보</legend>
          <select v-model="shippingPresetIndex" class="preset-select" @change="applyShippingPreset">
            <option value="" disabled>선택...</option>
            <optgroup v-if="shippingHistory.length" label="이전 배송지">
              <option v-for="(entry, i) in shippingHistory" :key="'h' + i" :value="`history:${i}`">
                {{ entry.recipientName }} / {{ entry.address1 }}
              </option>
            </optgroup>
            <optgroup label="샘플">
              <option v-for="(preset, i) in SHIPPING_PRESETS" :key="'s' + i" :value="`sample:${i}`">
                {{ preset.label }}
              </option>
            </optgroup>
          </select>
          <label>수령인<input v-model="form.recipientName" required /></label>
          <label>수령인 연락처<input v-model="form.recipientPhone" required /></label>
          <label>우편번호<input v-model="form.zipcode" /></label>
          <label>기본주소<input v-model="form.address1" required /></label>
          <label>상세주소<input v-model="form.address2" /></label>
        </fieldset>

        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

        <button type="submit" :disabled="submitting">{{ submitting ? '주문 처리 중...' : '결제하기' }}</button>
      </form>
    </template>
  </section>
</template>

<style scoped>
.checkout {
  max-width: 480px;
}
.error {
  color: #a33;
}
.summary {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fafafa;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin: 12px 0 20px;
  font-size: 14px;
}
.summary .total {
  font-weight: 700;
}
.order-form fieldset {
  border: none;
  padding: 0;
  margin: 0 0 16px;
}
.order-form legend {
  font-size: 13px;
  color: #888;
  margin-bottom: 8px;
  padding: 0;
}
.order-form label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  margin-bottom: 10px;
}
.order-form input {
  padding: 8px 10px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 14px;
}
.order-form input[readonly] {
  background: #f5f5f5;
  color: #666;
  cursor: default;
}
.preset-select {
  width: 100%;
  padding: 6px 8px;
  margin-bottom: 10px;
  border: 1px solid #cfe0f5;
  border-radius: 6px;
  background: #f3f8ff;
  font-size: 13px;
  color: #0056b3;
}
.order-form button {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 6px;
  background: #0056b3;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.order-form button:disabled {
  background: #a7c4e0;
  cursor: not-allowed;
}
</style>
