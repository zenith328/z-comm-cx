<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { sendChatMessage } from '../api/chat'
import { session } from '../stores/session'
import { getChatState, saveChatState } from '../utils/chatHistory'
import { renderMarkdownLite } from '../utils/markdown'
import { isQuotaExceededError } from '../utils/apiError'

interface ChatMessage {
  role: 'user' | 'assistant' | 'error'
  text: string
}

const route = useRoute()

const sessionId = ref<string | null>(null)
const messages = ref<ChatMessage[]>([
  { role: 'assistant', text: '안녕하세요! 주문 취소, 배송지 변경, 반품 등 무엇을 도와드릴까요? 주문번호와 함께 말씀해 주세요.' },
])
const input = ref('')
const sending = ref(false)
const scrollArea = ref<HTMLElement | null>(null)
const messageInput = ref<HTMLInputElement | null>(null)

onMounted(() => {
  if (session.current) {
    const saved = getChatState(session.current.name, session.current.phone)
    if (saved) {
      sessionId.value = saved.sessionId
      messages.value = saved.messages
    }
  }

  const orderNo = route.query.orderNo
  if (typeof orderNo === 'string' && orderNo) {
    input.value = `주문번호 ${orderNo} 관련해서 문의드립니다. `
    messageInput.value?.focus()
  }
})

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return

  messages.value.push({ role: 'user', text })
  input.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    const customer = session.current ? { name: session.current.name, phone: session.current.phone } : undefined
    const response = await sendChatMessage(sessionId.value, text, customer)
    sessionId.value = response.sessionId
    messages.value.push({ role: 'assistant', text: response.reply })
  } catch (error) {
    const text = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과해 지금은 답변할 수 없습니다. 잠시 후 다시 시도해 주세요.'
      : '요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    messages.value.push({ role: 'error', text })
  } finally {
    sending.value = false
    await scrollToBottom()
    messageInput.value?.focus()
    if (session.current && sessionId.value) {
      saveChatState(session.current.name, session.current.phone, sessionId.value, messages.value)
    }
  }
}

async function scrollToBottom() {
  await nextTick()
  scrollArea.value?.scrollTo({ top: scrollArea.value.scrollHeight, behavior: 'smooth' })
}

</script>

<template>
  <section class="chat">
    <div ref="scrollArea" class="messages">
      <div v-for="(m, i) in messages" :key="i" :class="['bubble', m.role]" v-html="renderMarkdownLite(m.text)"></div>
      <div v-if="sending" class="bubble assistant pending">답변을 준비하고 있어요...</div>
    </div>

    <form class="composer" @submit.prevent="send">
      <input
        ref="messageInput"
        v-model="input"
        type="text"
        placeholder="예: 주문번호 ORD-XXXX 취소해주세요"
        :disabled="sending"
      />
      <button type="submit" :disabled="sending || !input.trim()">보내기</button>
    </form>
  </section>
</template>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  height: 60vh;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #fafafa;
}
.bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  font-size: 14px;
}
.bubble.user {
  align-self: flex-end;
  background: #0056b3;
  color: #fff;
}
.bubble.assistant {
  align-self: flex-start;
  background: #fff;
  border: 1px solid #e0e0e0;
}
.bubble.error {
  align-self: flex-start;
  background: #fdeaea;
  border: 1px solid #f5b5b5;
  color: #a33;
}
.bubble.pending {
  opacity: 0.6;
}
.composer {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #e0e0e0;
  background: #fff;
}
.composer input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 14px;
}
.composer button {
  padding: 10px 18px;
  border: none;
  border-radius: 6px;
  background: #0056b3;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.composer button:disabled {
  background: #a7c4e0;
  cursor: not-allowed;
}
</style>
