import { reactive } from 'vue'
import { fetchCurrentSiteAuth, loginWithGoogle, logoutSiteAuth } from '../api/siteAuth'
import type { AxiosInstance } from 'axios'

export const siteAuth = reactive<{ email: string | null; checked: boolean; expired: boolean }>({
  email: null,
  checked: false,
  expired: false,
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
  siteAuth.expired = false
  return result.email
}

export async function logoutSite() {
  await logoutSiteAuth()
  siteAuth.email = null
}

/**
 * 모든 API 클라이언트에 붙이는 공통 401 처리기. 서버 재시작 등으로 사이트(Google) 세션이
 * 끊기면 어떤 API를 호출하든 401이 오는데, 그걸 개별 화면이 "로그인 실패"처럼 조용히 삼키지
 * 않고 항상 감지해서 사이트 로그인 화면으로 되돌리고 이유를 알려준다.
 * `/auth/**`는 로그인 흐름 자체(최초 미로그인 상태의 401 포함)라 이 처리에서 제외한다.
 */
export function attachSiteAuthInterceptor(client: AxiosInstance) {
  client.interceptors.response.use(
    (response) => response,
    (error) => {
      const url: string = error?.config?.url ?? ''
      if (error?.response?.status === 401 && !url.startsWith('/auth/')) {
        siteAuth.checked = true
        siteAuth.expired = true
        siteAuth.email = null
      }
      return Promise.reject(error)
    },
  )
}
