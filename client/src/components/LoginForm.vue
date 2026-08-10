<script setup lang="ts">
import { ref } from 'vue'

defineProps<{ showCancel?: boolean }>()
const emit = defineEmits<{
  submit: [payload: { name: string; phone: string }]
  cancel: []
}>()

const name = ref('')
const phone = ref('')

function handleSubmit() {
  if (!name.value.trim() || !phone.value.trim()) return
  emit('submit', { name: name.value.trim(), phone: phone.value.trim() })
}
</script>

<template>
  <form class="login-form" @submit.prevent="handleSubmit">
    <label>이름<input v-model="name" required autofocus /></label>
    <label>전화번호<input v-model="phone" required placeholder="010-0000-0000" /></label>
    <div class="actions">
      <button type="submit">로그인</button>
      <button v-if="showCancel" type="button" class="cancel" @click="emit('cancel')">취소</button>
    </div>
  </form>
</template>

<style scoped>
.login-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.login-form label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}
.login-form input {
  padding: 10px 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 14px;
}
.actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}
.actions button[type='submit'] {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 6px;
  background: #0056b3;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.actions button.cancel {
  padding: 12px 16px;
  border: 1px solid #ccc;
  border-radius: 6px;
  background: #fff;
  color: #444;
  font-size: 14px;
  cursor: pointer;
}
</style>
