import { http } from './http'
import type { MemberChargeRequest, MemberLoginResponse, MemberProfileUpdateRequest, MemberResponse } from './cs-types'
import type { PageResponse } from '../types/page'

export function fetchMembers(
  params: { page: number; size: number; search?: string },
): Promise<PageResponse<MemberResponse>> {
  return http.get<PageResponse<MemberResponse>>('/members', { params }).then((res) => res.data)
}

export function loginMember(name: string, phone: string) {
  return http.post<MemberLoginResponse>('/members/login', { name, phone }).then((res) => res.data)
}

export function updateMemberProfile(request: MemberProfileUpdateRequest) {
  return http.put<MemberResponse>('/members/profile', request).then((res) => res.data)
}

export function chargeMemberBalance(request: MemberChargeRequest) {
  return http.post<MemberResponse>('/members/cx-pay/charge', request).then((res) => res.data)
}
