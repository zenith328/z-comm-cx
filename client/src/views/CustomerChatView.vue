<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
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

const SpeechRecognitionCtor = window.SpeechRecognition || window.webkitSpeechRecognition
const voiceSupported = !!SpeechRecognitionCtor
const ttsSupported = 'speechSynthesis' in window

const listening = ref(false)
const speaking = ref(false)
// 대화 모드: 켜져 있는 동안은 인식 -> 자동 전송 -> 응답 음성 재생 -> 재청취를 반복한다.
const voiceMode = ref(false)
let recognition: SpeechRecognition | null = null

const voiceStatus = computed(() => {
  if (!voiceMode.value) return null
  if (listening.value && speaking.value) return '답변을 읽어드리는 중이에요. 말씀하시면 바로 들을게요.'
  if (listening.value) return '듣고 있어요...'
  if (speaking.value) return '답변을 읽어드리고 있어요...'
  if (sending.value) return '답변을 준비하고 있어요...'
  return '대화 모드 켜짐 - 말씀해 주세요'
})

function startListeningOnce() {
  if (!SpeechRecognitionCtor) return

  let gotResult = false
  const r = new SpeechRecognitionCtor()
  recognition = r
  r.lang = 'ko-KR'
  r.continuous = false
  r.interimResults = false

  r.onstart = () => {
    listening.value = true
  }
  r.onresult = (event) => {
    const transcript = event.results[event.results.length - 1][0].transcript.trim()
    // 답변을 읽어주는 도중이면, 마이크가 스피커 소리(AI 자신의 말)를 되받아 인식한 것일 수
    // 있다. 인식된 내용이 지금 읽고 있는 문장 안에 그대로 들어있으면 에코로 보고 무시한다
    // (완벽하지 않음 — 인식이 다르게 잘못 옮겨지면 걸러지지 않을 수 있다).
    if (speaking.value && isLikelyEcho(transcript, currentSpokenText)) {
      return
    }
    gotResult = true
    input.value = transcript
    // 진짜 사용자 발화로 판단되면 끼어들기(barge-in)로 처리하고, 재생 중이던 TTS는 멈춘다.
    if (speaking.value && ttsSupported) {
      window.speechSynthesis.cancel()
    }
  }
  r.onerror = (event) => {
    // 권한 거부 등 복구 불가능한 에러는 무한 재시도를 막기 위해 대화 모드 자체를 끈다.
    if (event.error === 'not-allowed' || event.error === 'service-not-allowed') {
      voiceMode.value = false
    }
  }
  r.onend = () => {
    listening.value = false
    // 이 사이 다른 인식 세션으로 교체됐다면(중복 시작 방지) 아무것도 하지 않는다.
    if (recognition !== r) return
    void handleRecognitionEnd(gotResult)
  }

  r.start()
}

async function handleRecognitionEnd(gotResult: boolean) {
  if (gotResult && input.value.trim()) {
    await send()
    if (voiceMode.value) {
      const last = messages.value[messages.value.length - 1]
      if (last && last.role !== 'user') {
        // TTS 재생이 끝나기를 기다리지 않고 곧바로 재청취를 시작한다. 이렇게 해야 답변을
        // 읽어주는 도중에도 마이크가 켜져 있어서 사용자가 끼어들어 말할 수 있다(barge-in).
        // speak()는 onresult에서 감지된 끼어들기로 인해 도중에 취소될 수 있다.
        void speak(last.text)
      }
    }
  }
  if (voiceMode.value) {
    startListeningOnce()
  } else {
    messageInput.value?.focus()
  }
}

let preferredVoice: SpeechSynthesisVoice | null = null

function refreshPreferredVoice() {
  if (!ttsSupported) return
  const koreanVoices = window.speechSynthesis.getVoices().filter((v) => v.lang.startsWith('ko'))
  if (koreanVoices.length === 0) {
    preferredVoice = null
    return
  }
  // 네트워크 기반(non-local) 음성이 OS 내장 로컬 음성보다 훨씬 자연스러운 경우가 많아
  // 우선 선택하고, 없으면 로컬 음성 중 첫 번째를 그대로 쓴다.
  preferredVoice = koreanVoices.find((v) => !v.localService) ?? koreanVoices[0]
}

// speak()로 지금 읽어주고 있는 텍스트. 에코 필터링(isLikelyEcho)이 마이크로 들어온
// 인식 결과와 비교하는 기준으로 쓴다.
let currentSpokenText = ''

function speak(text: string): Promise<void> {
  if (!ttsSupported || !text.trim()) return Promise.resolve()

  return new Promise((resolve) => {
    const spokenText = stripMarkdownForSpeech(text)
    const utterance = new SpeechSynthesisUtterance(spokenText)
    utterance.lang = 'ko-KR'
    if (preferredVoice) utterance.voice = preferredVoice
    currentSpokenText = spokenText
    speaking.value = true
    utterance.onend = () => {
      speaking.value = false
      currentSpokenText = ''
      resolve()
    }
    utterance.onerror = () => {
      speaking.value = false
      currentSpokenText = ''
      resolve()
    }
    window.speechSynthesis.speak(utterance)
  })
}

function normalizeForCompare(text: string): string {
  return text.replace(/[^\p{L}\p{N}]+/gu, '').toLowerCase()
}

/** 인식된 문장이 지금 읽어주고 있는 문장 안에 그대로 포함되면 에코(자기 목소리 되받음)로 본다. */
function isLikelyEcho(transcript: string, spokenText: string): boolean {
  const t = normalizeForCompare(transcript)
  const s = normalizeForCompare(spokenText)
  if (!t || !s) return false
  return s.includes(t)
}

function stripMarkdownForSpeech(text: string): string {
  return text
    .replace(/\[(.*?)\]\(.*?\)/g, '$1')
    .replace(/[*_`#>]/g, '')
    .replace(/^\s*-\s+/gm, '')
    .trim()
}

function toggleVoiceMode() {
  if (voiceMode.value) {
    voiceMode.value = false
    recognition?.abort()
    if (ttsSupported) window.speechSynthesis.cancel()
    speaking.value = false
  } else {
    voiceMode.value = true
    startListeningOnce()
  }
}

onUnmounted(() => {
  voiceMode.value = false
  recognition?.abort()
  if (ttsSupported) {
    window.speechSynthesis.cancel()
    window.speechSynthesis.onvoiceschanged = null
  }
})

onMounted(() => {
  if (ttsSupported) {
    // 목록이 비동기로 채워지는 브라우저(Chrome 등)를 위해 이벤트로도 갱신하고,
    // 이미 채워져 있는 브라우저(Safari 등)를 위해 한 번 즉시 시도한다.
    refreshPreferredVoice()
    window.speechSynthesis.onvoiceschanged = refreshPreferredVoice
  }

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

  void scrollToBottom()
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

    <div v-if="voiceStatus" class="voice-status" :class="{ recording: listening, speaking }">{{ voiceStatus }}</div>

    <form class="composer" @submit.prevent="send">
      <input
        ref="messageInput"
        v-model="input"
        type="text"
        placeholder="예: 주문번호 ORD-XXXX 취소해주세요"
        :disabled="sending"
      />
      <button
        v-if="voiceSupported"
        type="button"
        class="mic-btn"
        :class="{ recording: listening, active: voiceMode }"
        :title="voiceMode ? '대화 모드 끄기' : '대화 모드로 시작 (계속 말하면 자동 전송)'"
        @click="toggleVoiceMode"
      >🎤</button>
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
.mic-btn {
  background: #fff;
  border: 1px solid #ccc !important;
  color: #333;
  padding: 10px 14px !important;
}
.mic-btn.active {
  background: #e6f0fb;
  border-color: #0056b3 !important;
  color: #0056b3;
}
.mic-btn.recording {
  background: #fdeaea;
  border-color: #f5b5b5 !important;
  color: #a33;
  animation: mic-pulse 1.2s ease-in-out infinite;
}
@keyframes mic-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
.voice-status {
  padding: 6px 12px;
  font-size: 13px;
  color: #0056b3;
  background: #eef5fc;
  border-top: 1px solid #e0e0e0;
  text-align: center;
}
.voice-status.recording {
  color: #a33;
  background: #fdeaea;
}
.voice-status.speaking {
  color: #0a7d3a;
  background: #eaf7ef;
}
</style>
