export interface ShowhostResponse {
  script: string
  avatarImageUrl: string | null
  generatedAt: string
}

export interface ShowhostQnaResponse {
  answer: string
}

export interface AvatarSettingResponse {
  imageUrl: string | null
}

export interface ShowhostDiscountSetting {
  enabled: boolean
  discountRate: number
  validMinutes: number
}

export interface ShowhostOffer {
  id: number
  discountRate: number
  expiresAt: string
  // 서버가 계산한 남은 시간(초). 브라우저 시계와 무관하게 타이머를 돌리기 위해 이 값을 기준으로 쓴다.
  remainingSeconds: number
  used: boolean
  expired: boolean
}
