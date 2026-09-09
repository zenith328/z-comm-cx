<script setup lang="ts">
import { ref } from 'vue'
import { chargeBalance, session } from '../stores/session'

const emit = defineEmits<{ close: [] }>()

const chargeAmount = ref<number | null>(null)
const charging = ref(false)
const error = ref('')

async function handleCharge() {
  if (!chargeAmount.value || chargeAmount.value <= 0) {
    error.value = '충전할 금액을 입력해주세요.'
    return
  }
  charging.value = true
  error.value = ''
  try {
    await chargeBalance(chargeAmount.value)
    emit('close')
  } catch (e) {
    console.error(e)
    error.value = '충전에 실패했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    charging.value = false
  }
}
</script>

<template>
  <div class="cx-pay-charge">
    <h3>CX-Pay 충전</h3>
    <p class="balance">현재 잔액: {{ (session.current?.balance ?? 0).toLocaleString() }}원</p>
    <label class="amount-label">
      충전할 금액
      <input
        v-model.number="chargeAmount"
        type="number"
        min="1"
        step="1000"
        placeholder="예: 10000"
        :disabled="charging"
      />
    </label>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="actions">
      <button type="button" :disabled="charging" @click="handleCharge">{{ charging ? '충전 중...' : '충전하기' }}</button>
      <button type="button" class="close" @click="emit('close')">닫기</button>
    </div>
  </div>
</template>

<style scoped>
.cx-pay-charge h3 {
  margin: 0 0 4px;
}
.balance {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 700;
  color: #0056b3;
}
.amount-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #555;
  margin-bottom: 12px;
}
.amount-label input {
  padding: 10px 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 15px;
}
.error {
  color: #a80000;
  font-size: 13px;
  margin: 0 0 8px;
}
.actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}
.actions button:first-child {
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
.actions button.close {
  padding: 12px 16px;
  border: 1px solid #ccc;
  border-radius: 6px;
  background: #fff;
  color: #444;
  font-size: 14px;
  cursor: pointer;
}
.actions button:disabled {
  opacity: 0.6;
  cursor: default;
}
</style>
