<script setup lang="ts">
import { ref } from 'vue'
import type { Gender } from '../api/cs-types'

const props = defineProps<{ initialGender: Gender | null; initialBirthYear: number | null }>()
const emit = defineEmits<{ submit: [payload: { gender: Gender | null; birthYear: number | null }] }>()

// 나이 대신 출생년도를 받는 이유: 나이는 입력 시점 스냅샷이라 갱신 안 하면 시간이 지날수록
// stale해지지만, 출생년도는 불변값이라 다시 입력받을 필요 없이 항상 정확한 나이를 계산할 수 있다.
const currentYear = new Date().getFullYear()

const gender = ref<Gender | ''>(props.initialGender ?? '')
// input[type=number]에 v-model을 쓰면 Vue가 값을 자동으로 number로 캐스팅하므로
// (빈 값일 때만 '') string으로 단정하지 않는다.
const birthYear = ref<number | string>(props.initialBirthYear ?? '')

function handleSubmit() {
  const parsed = birthYear.value === '' || birthYear.value === null ? null : Number(birthYear.value)
  emit('submit', {
    gender: gender.value || null,
    birthYear: parsed !== null && !Number.isNaN(parsed) ? parsed : null,
  })
}
</script>

<template>
  <form class="gender-birthyear-form" @submit.prevent="handleSubmit">
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
    <slot></slot>
  </form>
</template>

<style scoped>
.gender-birthyear-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.gender-birthyear-form label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}
.gender-birthyear-form .optional {
  color: #999;
  font-weight: 400;
}
.gender-birthyear-form input,
.gender-birthyear-form select {
  padding: 10px 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 14px;
}
</style>
