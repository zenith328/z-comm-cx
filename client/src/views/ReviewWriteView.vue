<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createReview } from '../api/reviewApi'
import { fetchProduct } from '../api/products'
import ReviewExampleModal from '../components/ReviewExampleModal.vue'
import { session } from '../stores/session'
import { setBreadcrumbProductName } from '../stores/breadcrumb'
import type { Product } from '../types/product'
import type { ReviewCreateRequest } from '../types/review'

const route = useRoute()
const router = useRouter()

const productId = computed(() => Number(route.params.id))
const product = ref<Product | null>(null)
const loading = ref(false)
const loadError = ref('')

const form = reactive({
  content: '',
  rating: 5,
  hasPhoto: false,
})

const submitting = ref(false)
const errorMessage = ref('')
const showExampleModal = ref(false)

function applyExample(example: string) {
  form.content = example
  showExampleModal.value = false
}

function formatPrice(price: number | null): string {
  return price == null ? '-' : `${price.toLocaleString()}원`
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    product.value = await fetchProduct(productId.value)
    setBreadcrumbProductName(product.value.name)
  } catch {
    loadError.value = '상품 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!form.content.trim() || !product.value || !session.current) return
  submitting.value = true
  errorMessage.value = ''
  const request: ReviewCreateRequest = {
    productCode: product.value.productCode,
    memberId: session.current.name,
    content: form.content,
    rating: form.rating,
    hasPhoto: form.hasPhoto,
  }
  try {
    await createReview(request)
    router.push(`/products/${productId.value}`)
  } catch (error) {
    console.error(error)
    errorMessage.value = '리뷰 등록에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

onMounted(load)
onUnmounted(() => setBreadcrumbProductName(null))
</script>

<template>
  <div>
    <p v-if="loadError" class="error">{{ loadError }}</p>
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
        </div>
      </section>

      <form class="write-form" @submit.prevent="onSubmit">
        <h3>리뷰 작성하기</h3>
        <div class="fields">
          <label>
            작성자
            <input :value="session.current?.name ?? ''" type="text" disabled />
          </label>
          <label>
            별점
            <select v-model.number="form.rating">
              <option v-for="n in 5" :key="n" :value="n">{{ n }}</option>
            </select>
          </label>
          <label class="checkbox">
            <input v-model="form.hasPhoto" type="checkbox" />
            사진 첨부
          </label>
        </div>
        <div class="content-header">
          <label for="review-content">리뷰 내용</label>
          <button type="button" class="example-button" @click="showExampleModal = true">예시</button>
        </div>
        <textarea
          id="review-content"
          v-model="form.content"
          rows="4"
          required
          placeholder="상품에 대한 솔직한 후기를 남겨주세요"
        />
        <button type="submit" class="submit-button" :disabled="submitting">
          {{ submitting ? '등록 중...' : '등록' }}
        </button>
      </form>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <ReviewExampleModal :open="showExampleModal" @select="applyExample" @close="showExampleModal = false" />
    </template>
  </div>
</template>

<style scoped>
.product-summary {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}
.thumb-wrap {
  width: 120px;
  height: 120px;
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
  font-size: 16px;
}
.price {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}
.write-form {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 20px;
  background: #fafafa;
}
.write-form h3 {
  margin: 0 0 12px;
  font-size: 15px;
}
.fields {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
label {
  display: flex;
  flex-direction: column;
  font-size: 13px;
  gap: 4px;
}
label.checkbox {
  flex-direction: row;
  align-items: center;
  gap: 6px;
}
.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.example-button {
  padding: 3px 10px;
  font-size: 12px;
  cursor: pointer;
}
input,
select,
textarea {
  font-size: 13px;
  padding: 4px 6px;
}
input:disabled {
  background: #f5f5f5;
  color: #666;
}
textarea {
  width: 100%;
  font-family: inherit;
  resize: vertical;
  margin-bottom: 12px;
}
.submit-button {
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
}
.error {
  color: #a80000;
  font-size: 13px;
}
</style>
