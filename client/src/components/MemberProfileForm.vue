<script setup lang="ts">
import { ref } from 'vue'
import type { Gender } from '../api/cs-types'

const props = defineProps<{
  initialGender: Gender | null
  initialBirthYear: number | null
  initialHeightCm: number | null
  initialWeightKg: number | null
}>()
const emit = defineEmits<{
  submit: [
    payload: { gender: Gender | null; birthYear: number | null; heightCm: number | null; weightKg: number | null },
  ]
}>()

// 나이 대신 출생년도를 받는 이유: 나이는 입력 시점 스냅샷이라 갱신 안 하면 시간이 지날수록
// stale해지지만, 출생년도는 불변값이라 다시 입력받을 필요 없이 항상 정확한 나이를 계산할 수 있다.
const currentYear = new Date().getFullYear()

const gender = ref<Gender | ''>(props.initialGender ?? '')
// input[type=number]에 v-model을 쓰면 Vue가 값을 자동으로 number로 캐스팅하므로
// (빈 값일 때만 '') string으로 단정하지 않는다.
const birthYear = ref<number | string>(props.initialBirthYear ?? '')
const heightCm = ref<number | string>(props.initialHeightCm ?? '')
const weightKg = ref<number | string>(props.initialWeightKg ?? '')

function toIntOrNull(value: number | string): number | null {
  if (value === '' || value === null) return null
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}

function handleSubmit() {
  emit('submit', {
    gender: gender.value || null,
    birthYear: toIntOrNull(birthYear.value),
    heightCm: toIntOrNull(heightCm.value),
    weightKg: toIntOrNull(weightKg.value),
  })
}
</script>

<template>
  <form class="member-profile-form" @submit.prevent="handleSubmit">
    <label>
      성별 <span class="optional">(선택)</span>
      <select v-model="gender">
        <option value="">선택 안함</option>
        <option value="MALE">남성</option>
        <option value="FEMALE">여성</option>
      </select>
    </label>
    <label>
      출생년도 <span class="optional">(선택)</span>
      <input v-model="birthYear" type="number" min="1900" :max="currentYear" placeholder="예: 1998" />
    </label>
    <label>
      키(cm) <span class="optional">(선택, "내 체형 맞춤 핏 요약"에 사용)</span>
      <input v-model="heightCm" type="number" min="50" max="250" placeholder="예: 165" />
    </label>
    <label>
      몸무게(kg) <span class="optional">(선택)</span>
      <input v-model="weightKg" type="number" min="20" max="300" placeholder="예: 58" />
    </label>
    <slot></slot>
  </form>
</template>

<style scoped>
.member-profile-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.member-profile-form label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}
.member-profile-form .optional {
  color: #999;
  font-weight: 400;
}
.member-profile-form input,
.member-profile-form select {
  padding: 10px 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 14px;
}
</style>
