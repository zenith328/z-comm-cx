import { http } from './http'
import type { ChatResponse } from './cs-types'

export function sendChatMessage(
  sessionId: string | null,
  message: string,
  customer?: { name: string; phone: string },
) {
  return http
    .post<ChatResponse>(
      '/cs/chat',
      {
        sessionId,
        message,
        customerName: customer?.name,
        customerPhone: customer?.phone,
      },
      // Gemini Function Calling 왕복(1~2회) 때문에 원래도 몇 초씩 걸릴 수 있는 요청이라
      // "서버를 깨우는 중입니다" 콜드스타트 배너 대상에서 제외한다. 프론트의 "답변을 준비하고
      // 있어요..." 표시가 이미 로딩 상태를 안내하고 있다.
      { skipColdStartIndicator: true },
    )
    .then((res) => res.data)
}
