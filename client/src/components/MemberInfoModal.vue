<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MemberProfileForm from './MemberProfileForm.vue'
import CxPayChargeModal from './CxPayChargeModal.vue'
import { refreshSession, session, updateProfile } from '../stores/session'
import type { Gender } from '../api/cs-types'

const emit = defineEmits<{ close: [] }>()

const saving = ref(false)
const error = ref('')

const chargeModalOpen = ref(false)

// CS채팅으로 주문/취소/충전을 하면 잔액 같은 값이 서버에서 바뀌는데, 세션 캐시는 그대로라
// 화면을 열 때마다 최신값으로 다시 받아온다.
onMounted(() => {
  refreshSession()
})

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

    <section class="cx-pay">
      <span class="label">CX-Pay 잔액</span>
      <span class="balance">{{ (session.current?.balance ?? 0).toLocaleString() }}원</span>
      <button type="button" class="charge-btn" @click="chargeModalOpen = true">충전하기</button>
    </section>

    <div v-if="chargeModalOpen" class="nested-overlay" @click.self="chargeModalOpen = false">
      <div class="nested-modal">
        <CxPayChargeModal @close="chargeModalOpen = false" />
      </div>
    </div>

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
.cx-pay {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #f9fafb;
  font-size: 13px;
}
.cx-pay .label {
  color: #888;
  white-space: nowrap;
}
.cx-pay .balance {
  flex: 1;
  font-size: 16px;
  font-weight: 700;
  color: #0056b3;
  white-space: nowrap;
}
.charge-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: #0056b3;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.nested-overlay {
  position: fixed;
  inset: 0;
  z-index: 70;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
}
.nested-modal {
  width: 300px;
  padding: 24px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
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
