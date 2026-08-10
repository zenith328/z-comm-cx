export interface StoredChatMessage {
  role: 'user' | 'assistant' | 'error'
  text: string
}

interface ChatState {
  sessionId: string
  messages: StoredChatMessage[]
}

const MAX_MESSAGES = 50

function storageKey(name: string, phone: string): string {
  return `cs-auto-resolver:chat:${encodeURIComponent(name)}:${encodeURIComponent(phone)}`
}

export function getChatState(name: string, phone: string): ChatState | null {
  try {
    const raw = localStorage.getItem(storageKey(name, phone))
    return raw ? (JSON.parse(raw) as ChatState) : null
  } catch {
    return null
  }
}

export function saveChatState(name: string, phone: string, sessionId: string, messages: StoredChatMessage[]): void {
  localStorage.setItem(
    storageKey(name, phone),
    JSON.stringify({ sessionId, messages: messages.slice(-MAX_MESSAGES) }),
  )
}

