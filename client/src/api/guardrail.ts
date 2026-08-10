import { http } from './http'
import type { GuardrailResponse } from './cs-types'

export function getGuardrail() {
  return http.get<GuardrailResponse>('/guardrail').then((res) => res.data)
}
