import { reactive } from 'vue'
import { fetchCurrentSiteAuth, loginWithGoogle, logoutSiteAuth } from '../api/siteAuth'

export const siteAuth = reactive<{ email: string | null; checked: boolean }>({
  email: null,
  checked: false,
})

/** 부팅 시 이미 사이트 로그인 세션이 있는지 확인한다 (새로고침해도 다시 로그인하지 않도록). */
export async function checkSiteAuth() {
  try {
    const result = await fetchCurrentSiteAuth()
    siteAuth.email = result.email
  } catch {
    siteAuth.email = null
  } finally {
    siteAuth.checked = true
  }
}

export async function loginSiteWithGoogle(idToken: string): Promise<string> {
  const result = await loginWithGoogle(idToken)
  siteAuth.email = result.email
  return result.email
}

export async function logoutSite() {
  await logoutSiteAuth()
  siteAuth.email = null
}
