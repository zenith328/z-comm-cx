import { ref } from 'vue'
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios'

/**
 * Render 무료 플랜은 15분간 요청이 없으면 서버가 슬립 모드로 들어간다. 슬립 상태에서 온
 * 요청은 콜드스타트(최대 1분 가량)로 응답이 늦어지는데, 화면에 아무 표시가 없으면 "멈춘
 * 것처럼" 보인다. 요청이 일정 시간 이상 걸리면 이 플래그를 켜서 안내 배너를 띄운다.
 *
 * 여러 axios 인스턴스(client/src/api/*.ts)가 각자 만들어지므로, 전역에서 공유하는 상태와
 * 인터셉터를 여기 한 곳에 모아두고 각 인스턴스에 attach만 하면 되게 한다.
 */
export const isWakingUp = ref(false)

const WAKE_UP_THRESHOLD_MS = 10000

interface ColdStartConfig extends InternalAxiosRequestConfig {
  _coldStartTimer?: ReturnType<typeof setTimeout>
  _coldStartTriggered?: boolean
  /**
   * 이 요청은 원래도 오래 걸릴 수 있는(예: Gemini Function Calling 왕복) 요청이라
   * 콜드스타트 배너 트리거 대상에서 제외한다. 요청 config에 `{ skipColdStartIndicator: true }`를 넘기면 된다.
   */
  skipColdStartIndicator?: boolean
}

let slowRequestCount = 0

function settle(config?: ColdStartConfig) {
  if (!config) {
    return
  }
  clearTimeout(config._coldStartTimer)
  if (config._coldStartTriggered) {
    slowRequestCount = Math.max(0, slowRequestCount - 1)
    if (slowRequestCount === 0) {
      isWakingUp.value = false
    }
  }
}

export function attachColdStartIndicator(instance: AxiosInstance): void {
  instance.interceptors.request.use((config: ColdStartConfig) => {
    if (config.skipColdStartIndicator) {
      return config
    }
    config._coldStartTimer = setTimeout(() => {
      config._coldStartTriggered = true
      slowRequestCount += 1
      isWakingUp.value = true
    }, WAKE_UP_THRESHOLD_MS)
    return config
  })

  instance.interceptors.response.use(
    (response) => {
      settle(response.config as ColdStartConfig)
      return response
    },
    (error) => {
      settle(error.config as ColdStartConfig)
      return Promise.reject(error)
    },
  )
}
