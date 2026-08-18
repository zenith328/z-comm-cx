import { reactive } from 'vue'
import { loginMember, updateMemberProfile } from '../api/members'
import type { Gender } from '../api/cs-types'

const STORAGE_KEY = 'z-comm-cx:session'

export interface CustomerSession {
  name: string
  phone: string
  gender: Gender | null
  birthYear: number | null
  // 서버가 birthYear로 계산해서 내려주는 값. 세그먼트 매칭 등 기존 로직은 이 값을 그대로 쓴다.
  age: number | null
  // "내 체형 맞춤 핏 요약"에서만 쓰인다. 세그먼트/개인화 매칭에는 관여하지 않는다.
  heightCm: number | null
  weightKg: number | null
}

function readSession(): CustomerSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as CustomerSession) : null
  } catch {
    return null
  }
}

export const session = reactive<{ current: CustomerSession | null }>({
  current: readSession(),
})

function persist(next: CustomerSession) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  session.current = next
}

/**
 * 이름/전화번호로 로그인한다. 별도의 회원 로그인 체계가 없으므로 서버의 회원 정보(member 테이블)를
 * 이름+전화번호로 조회해서, 없으면 새로 등록(최초 로그인)하고 있으면 그대로 사용한다.
 * firstLogin이 true면 호출자가 성별/출생년도 입력창을 띄워야 한다.
 */
export async function login(customer: { name: string; phone: string }): Promise<{ firstLogin: boolean }> {
  const phone = customer.phone.replace(/\D/g, '')
  const result = await loginMember(customer.name, phone)
  persist({
    name: result.member.name,
    phone: result.member.phone,
    gender: result.member.gender,
    birthYear: result.member.birthYear,
    age: result.member.age,
    heightCm: result.member.heightCm,
    weightKg: result.member.weightKg,
  })
  return { firstLogin: result.firstLogin }
}

/** 최초 로그인 직후 추가 입력, "내 정보" 수정 둘 다 이 함수로 성별/출생년도/체형을 갱신한다. */
export async function updateProfile(
  gender: Gender | null,
  birthYear: number | null,
  heightCm: number | null,
  weightKg: number | null,
) {
  if (!session.current) return
  const updated = await updateMemberProfile({
    name: session.current.name,
    phone: session.current.phone,
    gender,
    birthYear,
    heightCm,
    weightKg,
  })
  persist({
    name: updated.name,
    phone: updated.phone,
    gender: updated.gender,
    birthYear: updated.birthYear,
    age: updated.age,
    heightCm: updated.heightCm,
    weightKg: updated.weightKg,
  })
}

export function logout() {
  localStorage.removeItem(STORAGE_KEY)
  session.current = null
}
