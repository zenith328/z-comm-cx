import { http } from './http'
import type { AvatarSettingResponse } from '../types/showhost'

export function fetchAvatarSetting(): Promise<AvatarSettingResponse> {
  return http.get<AvatarSettingResponse>('/avatar-showhost/setting').then((res) => res.data)
}

export function updateAvatarSetting(imageUrl: string): Promise<AvatarSettingResponse> {
  return http.put<AvatarSettingResponse>('/avatar-showhost/setting', { imageUrl }).then((res) => res.data)
}
