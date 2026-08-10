import { http } from './http'
import type { ChatResponse } from './cs-types'

export function sendChatMessage(
  sessionId: string | null,
  message: string,
  customer?: { name: string; phone: string },
) {
  return http
    .post<ChatResponse>('/cs/chat', {
      sessionId,
      message,
      customerName: customer?.name,
      customerPhone: customer?.phone,
    })
    .then((res) => res.data)
}
