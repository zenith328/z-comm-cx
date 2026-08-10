<script setup lang="ts">
import { ref } from 'vue'

defineProps<{ registering: boolean }>()
const emit = defineEmits<{ register: [url: string] }>()

const url = ref('')

function onSubmit() {
  if (!url.value.trim()) return
  emit('register', url.value.trim())
}
</script>

<template>
  <form class="register-form" @submit.prevent="onSubmit">
    <h2>상품 등록</h2>
    <p class="hint">상품 상세 페이지 URL을 입력하면 상품명/가격/이미지를 자동으로 가져옵니다.</p>
    <div class="fields">
      <input v-model="url" type="url" placeholder="https://www.zerogram.co.kr/product/..." required />
      <button type="submit" :disabled="registering">{{ registering ? '등록 중...' : '등록' }}</button>
    </div>
  </form>
</template>

<style scoped>
.register-form {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 24px;
  background: #fafafa;
}
h2 {
  margin: 0 0 4px;
  font-size: 16px;
}
.hint {
  margin: 0 0 12px;
  font-size: 12px;
  color: #777;
}
.fields {
  display: flex;
  gap: 8px;
}
.fields input {
  flex: 1;
  padding: 6px 8px;
  font-size: 13px;
}
button {
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}
</style>
