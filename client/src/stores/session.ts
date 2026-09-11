import { reactive } from 'vue'
import { chargeMemberBalance, loginMember, updateMemberProfile } from '../api/members'
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
  // CX-Pay(이 사이트의 유일한 결제수단) 잔액.
  balance: number
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
    balance: result.member.balance,
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
    balance: updated.balance,
  })
}

/**
 * 서버에 있는 최신 회원 정보(특히 CX-Pay 잔액)로 세션을 다시 채운다. CS채팅에서 주문/취소/충전을
 * 하면 백엔드 데이터는 바뀌지만 이 세션 캐시는 그대로라, "내 정보"를 열 때마다 이걸로 갱신해준다.
 */
export async function refreshSession() {
  if (!session.current) return
  const result = await loginMember(session.current.name, session.current.phone)
  persist({
    name: result.member.name,
    phone: result.member.phone,
    gender: result.member.gender,
    birthYear: result.member.birthYear,
    age: result.member.age,
    heightCm: result.member.heightCm,
    weightKg: result.member.weightKg,
    balance: result.member.balance,
  })
}

/** CX-Pay 충전. "내 정보" 화면과 CS채팅 둘 다 결국 이 API를 거친다(채팅은 백엔드 tool을 통해서). */
export async function chargeBalance(amount: number) {
  if (!session.current) return
  const updated = await chargeMemberBalance({ name: session.current.name, phone: session.current.phone, amount })
  persist({ ...session.current, balance: updated.balance })
}

export function logout() {
  localStorage.removeItem(STORAGE_KEY)
  session.current = null
}
