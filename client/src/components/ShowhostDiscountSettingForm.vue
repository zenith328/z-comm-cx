<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { discountedPrice, fetchShowhostDiscount, updateShowhostDiscount } from '../api/showhostApi'
import { extractErrorMessage } from '../utils/apiError'

// 운영자 상품상세관리 "쇼호스트 방송" 탭 상단의 방송 특가 설정. 저장하면 saved를 보내서, 부모가
// 아래 방송 미리보기(ShowhostBroadcastPanel)를 다시 불러와 안내 멘트에 바로 반영되게 한다.
const props = defineProps<{ productCode: string; price: number | null }>()
const emit = defineEmits<{ saved: [] }>()

const form = reactive({ enabled: false, discountRate: 10, validMinutes: 30 })
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const errorMessage = ref('')

const previewPrice = computed(() =>
  props.price != null && form.discountRate >= 1 && form.discountRate <= 90
    ? discountedPrice(props.price, form.discountRate)
    : null,
)

async function load() {
  loading.value = true
  errorMessage.value = ''
  message.value = ''
  try {
    const setting = await fetchShowhostDiscount(props.productCode)
    Object.assign(form, setting)
  } catch (error) {
    console.error(error)
    errorMessage.value = '방송 특가 설정을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  errorMessage.value = ''
  message.value = ''
  try {
    const saved = await updateShowhostDiscount(props.productCode, { ...form })
    Object.assign(form, saved)
    message.value = '저장했습니다.'
    emit('saved')
  } catch (error) {
    errorMessage.value = extractErrorMessage(error, '방송 특가 설정을 저장하지 못했습니다.')
  } finally {
    saving.value = false
  }
}

watch(() => props.productCode, load, { immediate: true })
</script>

<template>
  <section class="discount-setting">
    <h3>방송 특가</h3>
    <p class="hint">
      방송을 본 고객에게만, 방송에 들어온 순간부터 정해진 시간 동안 쓸 수 있는 할인을 줍니다.
      고객당 상품별로 하루 1번 발급되고 주문 1건에만 쓸 수 있으며, 주문을 취소해도 되살아나지 않습니다.
      설정을 바꿔도 이미 발급된 특가의 할인율은 그대로입니다.
    </p>

    <p v-if="loading" class="hint">불러오는 중...</p>
    <form v-else class="setting-form" @submit.prevent="save">
      <label class="toggle">
        <input v-model="form.enabled" type="checkbox" />
        방송 특가 사용
      </label>
      <label>
        할인율
        <input v-model.number="form.discountRate" type="number" min="1" max="90" required />
        %
      </label>
      <label>
        유효 시간
        <input v-model.number="form.validMinutes" type="number" min="5" max="180" required />
        분
      </label>
      <span v-if="price != null && previewPrice != null" class="preview">
        {{ price.toLocaleString() }}원 → <strong>{{ previewPrice.toLocaleString() }}원</strong>
      </span>
      <button type="submit" :disabled="saving">{{ saving ? '저장 중...' : '저장' }}</button>
    </form>
    <p v-if="message" class="message">{{ message }}</p>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </section>
</template>

<style scoped>
.discount-setting {
  margin-bottom: 20px;
  padding: 16px 18px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  background: #fafbfc;
}
.discount-setting h3 {
  margin: 0 0 6px;
  font-size: 15px;
}
.hint {
  margin: 0 0 12px;
  font-size: 12px;
  color: #666;
  line-height: 1.5;
}
.setting-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
}
.setting-form label {
  display: flex;
  align-items: center;
  gap: 6px;
}
.setting-form input[type='number'] {
  width: 70px;
  padding: 6px 8px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 13px;
}
.preview {
  color: #555;
}
.preview strong {
  color: #e63946;
}
.setting-form button {
  margin-left: auto;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background: #0056b3;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.setting-form button:disabled {
  opacity: 0.6;
  cursor: default;
}
.message {
  margin: 8px 0 0;
  font-size: 12px;
  color: #1a7f37;
}
.error {
  margin: 8px 0 0;
  font-size: 12px;
  color: #a80000;
}
</style>
