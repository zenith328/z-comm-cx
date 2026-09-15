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
