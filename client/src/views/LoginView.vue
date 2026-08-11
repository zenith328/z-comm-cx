<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginForm from '../components/LoginForm.vue'
import MemberProfilePrompt from '../components/MemberProfilePrompt.vue'
import { login } from '../stores/session'

const route = useRoute()
const router = useRouter()

const step = ref<'form' | 'profile'>('form')
const error = ref('')

async function handleSubmit(payload: { name: string; phone: string }) {
  error.value = ''
  try {
    const { firstLogin } = await login(payload)
    if (firstLogin) {
      step.value = 'profile'
    } else {
      goNext()
    }
  } catch (e) {
    console.error(e)
    error.value = '로그인에 실패했습니다. 잠시 후 다시 시도해주세요.'
  }
}

function goNext() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/products'
  router.push(redirect)
}
</script>

<template>
  <section class="login">
    <h2>로그인</h2>
    <template v-if="step === 'form'">
      <p class="hint">이름과 전화번호를 입력하면 상품 조회·리뷰 작성, 주문, CS 채팅을 이용할 수 있습니다.</p>
      <p v-if="error" class="error">{{ error }}</p>
      <LoginForm @submit="handleSubmit" />
    </template>
    <MemberProfilePrompt v-else @done="goNext" />
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
.error {
  color: #a80000;
  font-size: 13px;
  margin-bottom: 12px;
}
</style>
