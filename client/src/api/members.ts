import { http } from './http'
import type { MemberLoginResponse, MemberProfileUpdateRequest, MemberResponse } from './cs-types'

export function loginMember(name: string, phone: string) {
  return http.post<MemberLoginResponse>('/members/login', { name, phone }).then((res) => res.data)
}

export function updateMemberProfile(request: MemberProfileUpdateRequest) {
  return http.put<MemberResponse>('/members/profile', request).then((res) => res.data)
}
