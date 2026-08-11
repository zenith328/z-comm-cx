<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Breadcrumb from './components/Breadcrumb.vue'
import GoogleSignInButton from './components/GoogleSignInButton.vue'
import LoginForm from './components/LoginForm.vue'
import { login, logout, session } from './stores/session'
import { checkSiteAuth, loginSiteWithGoogle, logoutSite, siteAuth } from './stores/siteAuth'
import { fetchSiteAuthConfig } from './api/siteAuth'
import { getGuardrail } from './api/guardrail'
import type { GuardrailResponse } from './api/cs-types'

const route = useRoute()
const router = useRouter()
const activeGroup = computed<'customer' | 'admin'>(() => (route.path.startsWith('/admin') ? 'admin' : 'customer'))
const isLoginPage = computed(() => route.path === '/login')

const googleClientId = ref('')
const siteLoginError = ref('')
const siteLoginLoading = ref(false)

onMounted(async () => {
  const [config] = await Promise.all([fetchSiteAuthConfig(), checkSiteAuth()])
  googleClientId.value = config.googleClientId
})

async function handleGoogleCredential(idToken: string) {
  siteLoginError.value = ''
  siteLoginLoading.value = true
  try {
    await loginSiteWithGoogle(idToken)
    router.push('/admin/guide')
  } catch (error: any) {
    siteLoginError.value =
      error?.response?.status === 403
        ? '허용되지 않은 계정입니다. 관리자에게 접근 권한을 요청해 주세요.'
        : '로그인에 실패했습니다. 다시 시도해 주세요.'
  } finally {
    siteLoginLoading.value = false
  }
}

async function handleSiteLogout() {
  logout()
  await logoutSite()
}

const logoutConfirmOpen = ref(false)
const loginLayerOpen = ref(false)

const guardrailLayerOpen = ref(false)
const guardrailInfo = ref<GuardrailResponse | null>(null)
const guardrailError = ref('')

async function openGuardrailLayer() {
  guardrailLayerOpen.value = true
  guardrailError.value = ''
  try {
    guardrailInfo.value = await getGuardrail()
  } catch {
    guardrailError.value = '가드레일 규칙을 불러오지 못했습니다.'
  }
}

function confirmLogout() {
  logoutConfirmOpen.value = false
  logout()
  loginLayerOpen.value = true
}

function goCustomer() {
  if (session.current) {
    router.push('/products')
  } else {
    loginLayerOpen.value = true
  }
}

function handleLoginLayerSubmit(payload: { name: string; phone: string }) {
  login(payload)
  loginLayerOpen.value = false
  router.push('/products')
}

function handleLoginLayerCancel() {
  loginLayerOpen.value = false
  router.push('/admin/guide')
}
</script>

<template>
  <main v-if="!siteAuth.checked" class="page site-gate-page">
    <p>불러오는 중...</p>
  </main>

  <main v-else-if="!siteAuth.email" class="page site-gate-page">
    <h1>Zenith Commerce CX</h1>
    <p class="site-gate-hint">이 사이트는 허용된 Google 계정으로만 접근할 수 있습니다.</p>

    <div v-if="siteLoginLoading" class="site-gate-loading">
      <span class="spinner"></span>
      <p>로그인 처리 중입니다...</p>
      <p class="site-gate-loading-sub">서버가 잠들어 있었다면 깨어나는 데 최대 1분 정도 걸릴 수 있어요.</p>
    </div>
    <GoogleSignInButton v-else-if="googleClientId" :client-id="googleClientId" @credential="handleGoogleCredential" />
    <p v-else class="site-gate-error">
      GOOGLE_CLIENT_ID가 설정되지 않았습니다. application-local.yml의 google.client-id를 채워주세요.
    </p>
    <p v-if="siteLoginError" class="site-gate-error">{{ siteLoginError }}</p>
  </main>

  <main v-else class="page">
    <div class="header-row">
      <h1>Zenith Commerce CX</h1>
      <div class="site-user-box">
        <span>{{ siteAuth.email }}</span>
        <button type="button" @click="handleSiteLogout">사이트 로그아웃</button>
      </div>
    </div>

    <template v-if="!isLoginPage">
      <nav class="top-tabs">
        <a href="#" class="top-tab" :class="{ active: activeGroup === 'customer' }" @click.prevent="goCustomer">고객화면</a>
        <RouterLink to="/admin/guide" class="top-tab" :class="{ active: activeGroup === 'admin' }">운영자어드민</RouterLink>
      </nav>

      <nav class="sub-tabs">
        <template v-if="activeGroup === 'customer'">
          <RouterLink to="/products" active-class="active">상품목록</RouterLink>
          <RouterLink to="/orders" active-class="active">주문목록</RouterLink>
          <RouterLink to="/chat" active-class="active">CS채팅</RouterLink>
          <div v-if="session.current" class="user-box">
            <span>{{ session.current.name }}님</span>
            <div class="logout-wrapper">
              <button type="button" @click="logoutConfirmOpen = !logoutConfirmOpen">로그아웃</button>
              <div v-if="logoutConfirmOpen" class="logout-popover">
                <p>로그아웃 하시겠습니까?</p>
                <div class="popover-actions">
                  <button type="button" class="confirm" @click="confirmLogout">확인</button>
                  <button type="button" @click="logoutConfirmOpen = false">취소</button>
                </div>
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <RouterLink to="/admin/guide" active-class="active">사용법</RouterLink>
          <RouterLink to="/admin/products" active-class="active">상품관리</RouterLink>
          <RouterLink to="/admin/reviews" active-class="active">리뷰관리</RouterLink>
          <RouterLink to="/admin/orders" active-class="active">주문관리</RouterLink>
          <RouterLink to="/admin/tickets" active-class="active">CS목록</RouterLink>
          <RouterLink to="/admin/db" active-class="active">DB관리</RouterLink>
          <button type="button" class="guardrail-tab-button" @click="openGuardrailLayer">AI 자동이관 기준</button>
        </template>
      </nav>

      <Breadcrumb v-if="activeGroup === 'customer'" />
    </template>

    <RouterView />

    <div v-if="logoutConfirmOpen" class="overlay" @click="logoutConfirmOpen = false"></div>

    <div v-if="loginLayerOpen" class="modal-overlay" @click.self="loginLayerOpen = false">
      <div class="login-modal">
        <h3>로그인</h3>
        <p class="hint">고객화면을 이용하려면 이름과 전화번호를 입력해 주세요.</p>
        <LoginForm show-cancel @submit="handleLoginLayerSubmit" @cancel="handleLoginLayerCancel" />
      </div>
    </div>

    <div v-if="guardrailLayerOpen" class="modal-overlay" @click.self="guardrailLayerOpen = false">
      <div class="login-modal">
        <h3>AI 자동이관 기준</h3>
        <p class="hint">AI가 직접 처리하지 않고 상담원에게 자동으로 이관하는 하드 룰입니다.</p>
        <p v-if="guardrailError" class="guardrail-error">{{ guardrailError }}</p>
        <dl v-else-if="guardrailInfo" class="guardrail-rules">
          <div class="guardrail-rule">
            <dt>배송 후 취소 요청</dt>
            <dd>이미 배송이 시작된 주문의 취소 요청은 금액과 무관하게 즉시 이관</dd>
          </div>
          <div class="guardrail-rule">
            <dt>반품 접수 기한</dt>
            <dd>배송완료 후 {{ guardrailInfo.returnWindowDays }}일 경과 시 반품을 자동 거절하고 이관 (금액과 무관)</dd>
          </div>
        </dl>
        <p v-else class="hint">불러오는 중...</p>
        <button type="button" class="guardrail-close" @click="guardrailLayerOpen = false">닫기</button>
      </div>
    </div>
  </main>
</template>

<style scoped>
.page {
  max-width: 1350px;
  margin: 0 auto;
  padding: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}
h1 {
  margin-bottom: 4px;
}
.site-gate-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
  gap: 16px;
}
.site-gate-hint {
  color: #666;
  font-size: 14px;
  margin: 0 0 8px;
}
.site-gate-error {
  color: #a80000;
  font-size: 13px;
}
.site-gate-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.site-gate-loading p {
  margin: 0;
  font-size: 14px;
  color: #444;
}
.site-gate-loading-sub {
  font-size: 12px !important;
  color: #999 !important;
}
.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e0e0e0;
  border-top-color: #0056b3;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 4px;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.header-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.site-user-box {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #888;
}
.site-user-box button {
  padding: 3px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
}
.user-box {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #444;
  margin-left: auto;
  padding-bottom: 8px;
}
.user-box button {
  padding: 4px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
}
.logout-wrapper {
  position: relative;
}
.logout-popover {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  z-index: 50;
  width: 180px;
  padding: 12px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}
.logout-popover p {
  margin: 0 0 10px;
  font-size: 13px;
  color: #333;
}
.popover-actions {
  display: flex;
  gap: 6px;
}
.popover-actions button {
  flex: 1;
  padding: 6px 0;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
}
.popover-actions button.confirm {
  background: #0056b3;
  border-color: #0056b3;
  color: #fff;
}
.overlay {
  position: fixed;
  inset: 0;
  z-index: 40;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
}
.login-modal {
  width: 320px;
  padding: 24px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
}
.login-modal h3 {
  margin: 0 0 4px;
}
.login-modal .hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: #777;
}
.top-tabs {
  display: flex;
  gap: 8px;
  margin: 16px 0 0;
}
.top-tab {
  padding: 8px 18px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  border-radius: 6px 6px 0 0;
  color: #666;
  background: #f0f0f0;
}
.top-tab.active {
  color: #fff;
  background: #0056b3;
}
.sub-tabs {
  display: flex;
  gap: 4px;
  margin: 0 0 20px;
  padding: 10px 12px 0;
  background: #f7f9fc;
  border: 1px solid #e0e0e0;
  border-top: none;
  border-radius: 0 6px 6px 6px;
}
.sub-tabs a {
  padding: 8px 16px;
  font-size: 14px;
  text-decoration: none;
  border-bottom: 2px solid transparent;
  color: #666;
}
.sub-tabs a.active {
  color: #0056b3;
  border-bottom-color: #0056b3;
  font-weight: 600;
}
.guardrail-tab-button {
  margin-left: auto;
  padding: 8px 16px;
  font-size: 14px;
  border: none;
  background: none;
  color: #666;
  cursor: pointer;
}
.guardrail-tab-button:hover {
  color: #0056b3;
}
.guardrail-rules {
  margin: 0 0 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.guardrail-rule dt {
  font-size: 12px;
  font-weight: 600;
  color: #888;
  margin-bottom: 2px;
}
.guardrail-rule dd {
  margin: 0;
  font-size: 14px;
  color: #333;
}
.guardrail-error {
  color: #c0392b;
  font-size: 13px;
}
.guardrail-close {
  width: 100%;
  padding: 8px 0;
  border: 1px solid #ccc;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
}
</style>
