<script setup lang="ts">
import { ref, watch } from 'vue'
import '../styles/admin.css'
import Pagination from '../components/Pagination.vue'
import { useMembers } from '../composables/useMembers'
import type { MemberResponse } from '../api/cs-types'
import { maskName, maskPhone } from '../utils/mask'
import { formatDateTime } from '../utils/format'

const { members, loading, errorMessage, page, totalPages, totalElements, refresh, goToPage, setSearchQuery } =
  useMembers()

const searchInput = ref('')

let debounceTimer: ReturnType<typeof setTimeout> | undefined
watch(searchInput, (value) => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => setSearchQuery(value), 300)
})

function genderLabel(gender: MemberResponse['gender']): string {
  if (gender === 'MALE') return '남성'
  if (gender === 'FEMALE') return '여성'
  return '-'
}

function bodyLabel(member: MemberResponse): string {
  if (member.heightCm == null && member.weightKg == null) return '-'
  return `${member.heightCm ?? '-'}cm / ${member.weightKg ?? '-'}kg`
}
</script>

<template>
  <section class="admin-page">
    <h2 class="admin-title">회원관리</h2>

    <div class="admin-toolbar">
      <button type="button" :disabled="loading" @click="refresh">새로고침</button>
      <label class="admin-filter admin-filter-plain">
        <span class="admin-filter-label">검색</span>
        <input v-model="searchInput" type="text" placeholder="이름 또는 전화번호" />
      </label>
    </div>

    <p v-if="errorMessage" class="admin-error">{{ errorMessage }}</p>
    <p v-else-if="loading">불러오는 중...</p>
    <p v-else-if="members.length === 0">조건에 맞는 회원이 없습니다.</p>

    <template v-else>
      <table class="admin-table">
        <thead>
          <tr>
            <th>이름</th>
            <th>전화번호</th>
            <th>성별</th>
            <th>연령</th>
            <th>체형</th>
            <th>CX-Pay 잔액</th>
            <th>가입일</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="member in members" :key="`${member.name}-${member.phone}`">
            <td>{{ maskName(member.name) }}</td>
            <td>{{ maskPhone(member.phone) }}</td>
            <td>{{ genderLabel(member.gender) }}</td>
            <td>{{ member.age != null ? `${member.age}세` : '-' }}</td>
            <td>{{ bodyLabel(member) }}</td>
            <td class="balance">{{ member.balance.toLocaleString() }}원</td>
            <td>{{ formatDateTime(member.createdAt) }}</td>
          </tr>
        </tbody>
      </table>

      <Pagination :page="page" :total-pages="totalPages" :total-elements="totalElements" @change="goToPage" />
    </template>
  </section>
</template>

<style scoped>
.balance {
  white-space: nowrap;
  font-weight: 600;
  color: #0056b3;
}
</style>
