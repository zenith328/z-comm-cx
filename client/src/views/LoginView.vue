<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import LoginForm from '../components/LoginForm.vue'
import { login } from '../stores/session'

const route = useRoute()
const router = useRouter()

function handleSubmit(payload: { name: string; phone: string }) {
  login(payload)
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/products'
  router.push(redirect)
}
</script>

<template>
  <section class="login">
    <h2>로그인</h2>
    <p class="hint">이름과 전화번호를 입력하면 상품 조회·리뷰 작성, 주문, CS 채팅을 이용할 수 있습니다.</p>
    <LoginForm @submit="handleSubmit" />
  </section>
</template>

<style scoped>
.login {
  max-width: 360px;
  margin: 40px auto;
}
.hint {
  color: #777;
  font-size: 13px;
  margin-bottom: 20px;
}
</style>
