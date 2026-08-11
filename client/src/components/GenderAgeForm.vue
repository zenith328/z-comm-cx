<script setup lang="ts">
import { ref } from 'vue'
import type { Gender } from '../api/cs-types'

const props = defineProps<{ initialGender: Gender | null; initialAge: number | null }>()
const emit = defineEmits<{ submit: [payload: { gender: Gender | null; age: number | null }] }>()

const gender = ref<Gender | ''>(props.initialGender ?? '')
// input[type=number]에 v-model을 쓰면 Vue가 값을 자동으로 number로 캐스팅하므로
// (빈 값일 때만 '') string으로 단정하지 않는다.
const age = ref<number | string>(props.initialAge ?? '')

function handleSubmit() {
  const parsedAge = age.value === '' || age.value === null ? null : Number(age.value)
  emit('submit', {
    gender: gender.value || null,
    age: parsedAge !== null && !Number.isNaN(parsedAge) ? parsedAge : null,
  })
}
</script>

<template>
  <form class="gender-age-form" @submit.prevent="handleSubmit">
    <label>
      성별 <span class="optional">(선택)</span>
      <select v-model="gender">
        <option value="">선택 안함</option>
        <option value="MALE">남성</option>
        <option value="FEMALE">여성</option>
      </select>
    </label>
    <label>
      연령 <span class="optional">(선택)</span>
      <input v-model="age" type="number" min="0" max="120" placeholder="예: 28" />
    </label>
    <slot></slot>
  </form>
</template>

<style scoped>
.gender-age-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.gender-age-form label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}
.gender-age-form .optional {
  color: #999;
  font-weight: 400;
}
.gender-age-form input,
.gender-age-form select {
  padding: 10px 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 14px;
}
</style>
