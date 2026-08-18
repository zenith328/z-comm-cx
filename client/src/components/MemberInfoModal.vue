<script setup lang="ts">
import { ref } from 'vue'
import MemberProfileForm from './MemberProfileForm.vue'
import { session, updateProfile } from '../stores/session'
import type { Gender } from '../api/cs-types'

const emit = defineEmits<{ close: [] }>()

const saving = ref(false)
const error = ref('')

function genderLabel(gender: Gender | null | undefined): string {
  if (gender === 'MALE') return '남성'
  if (gender === 'FEMALE') return '여성'
  return '선택 안함'
}

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
    // session.gender/age가 갱신되면 상품상세 등 이를 구독하는 화면이 자동으로 다시 조회한다.
    emit('close')
  } catch (e) {
    console.error(e)
    error.value = '저장에 실패했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="member-info">
    <h3>내 정보</h3>
    <dl class="readonly-info">
      <dt>이름</dt>
      <dd>{{ session.current?.name }}</dd>
      <dt>전화번호</dt>
      <dd>{{ session.current?.phone }}</dd>
      <dt>현재 성별</dt>
      <dd>{{ genderLabel(session.current?.gender) }}</dd>
      <dt>현재 연령</dt>
      <dd>{{ session.current?.age != null ? `${session.current.age}세` : '선택 안함' }}</dd>
      <dt>현재 체형</dt>
      <dd>
        {{
          session.current?.heightCm != null || session.current?.weightKg != null
            ? `${session.current?.heightCm ?? '-'}cm / ${session.current?.weightKg ?? '-'}kg`
            : '선택 안함'
        }}
      </dd>
    </dl>
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
        <button type="button" class="close" @click="emit('close')">닫기</button>
      </div>
    </MemberProfileForm>
  </div>
</template>

<style scoped>
.member-info h3 {
  margin: 0 0 12px;
}
.readonly-info {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 12px;
  margin: 0 0 16px;
  font-size: 13px;
}
.readonly-info dt {
  color: #888;
}
.readonly-info dd {
  margin: 0;
  color: #333;
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
