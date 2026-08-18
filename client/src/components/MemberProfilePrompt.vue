<script setup lang="ts">
import { ref } from 'vue'
import MemberProfileForm from './MemberProfileForm.vue'
import { session, updateProfile } from '../stores/session'
import type { Gender } from '../api/cs-types'

const emit = defineEmits<{ done: [] }>()

const saving = ref(false)
const error = ref('')

async function handleSubmit(payload: {
  gender: Gender | null
  birthYear: number | null
  heightCm: number | null
  weightKg: number | null
}) {
  saving.value = true
  error.value = ''
  try {
    await updateProfile(payload.gender, payload.birthYear, payload.heightCm, payload.weightKg)
    emit('done')
  } catch (e) {
    console.error(e)
    error.value = '저장에 실패했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="profile-prompt">
    <h3>추가 정보 입력</h3>
    <p class="hint">처음 로그인하셨네요. 성별/출생년도를 입력하면 고객님께 맞는 상품 설명을 보여드릴 수 있어요.</p>
    <MemberProfileForm
      :initial-gender="session.current?.gender ?? null"
      :initial-birth-year="session.current?.birthYear ?? null"
      :initial-height-cm="session.current?.heightCm ?? null"
      :initial-weight-kg="session.current?.weightKg ?? null"
      @submit="handleSubmit"
    >
      <p v-if="error" class="error">{{ error }}</p>
      <div class="actions">
        <button type="submit" :disabled="saving">{{ saving ? '저장 중...' : '저장' }}</button>
        <button type="button" class="skip" :disabled="saving" @click="emit('done')">건너뛰기</button>
      </div>
    </MemberProfileForm>
  </div>
</template>

<style scoped>
.profile-prompt h3 {
  margin: 0 0 4px;
}
.hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: #777;
}
.error {
  color: #a80000;
  font-size: 13px;
  margin: 0;
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
.actions button.skip {
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
