import { reactive } from 'vue'

const STORAGE_KEY = 'z-comm-cx:session'

export interface CustomerSession {
  name: string
  phone: string
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

export function login(customer: CustomerSession) {
  const normalized: CustomerSession = { ...customer, phone: customer.phone.replace(/\D/g, '') }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized))
  session.current = normalized
}

export function logout() {
  localStorage.removeItem(STORAGE_KEY)
  session.current = null
}
