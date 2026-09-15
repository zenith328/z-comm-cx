import { onMounted, onUnmounted, ref } from 'vue'

// cs-chat-voice-input에서 검증된 것과 동일한 방식 — 브라우저 내장 음성합성(Web Speech API)만
// 사용하므로 Gemini API·백엔드와 무관하고 추가 비용이 없다.
const ttsSupported = 'speechSynthesis' in window

function stripMarkdownForSpeech(text: string): string {
  return text
    .replace(/\[(.*?)\]\(.*?\)/g, '$1')
    .replace(/[*_`#>]/g, '')
    .replace(/^\s*-\s+/gm, '')
    .trim()
}

/** 브라우저 내장 TTS로 텍스트를 읽어주는 재사용 가능한 composable. */
export function useTts() {
  const speaking = ref(false)
  let preferredVoice: SpeechSynthesisVoice | null = null

  function refreshPreferredVoice() {
    if (!ttsSupported) return
    const koreanVoices = window.speechSynthesis.getVoices().filter((v) => v.lang.startsWith('ko'))
    if (koreanVoices.length === 0) {
      preferredVoice = null
      return
    }
    // 네트워크 기반(non-local) 음성이 OS 내장 로컬 음성보다 자연스러운 경우가 많아 우선 선택한다.
    preferredVoice = koreanVoices.find((v) => !v.localService) ?? koreanVoices[0]
  }

  function speak(text: string): Promise<void> {
    if (!ttsSupported || !text.trim()) return Promise.resolve()

    return new Promise((resolve) => {
      const utterance = new SpeechSynthesisUtterance(stripMarkdownForSpeech(text))
      utterance.lang = 'ko-KR'
      if (preferredVoice) utterance.voice = preferredVoice
      speaking.value = true
      utterance.onend = () => {
        speaking.value = false
        resolve()
      }
      utterance.onerror = () => {
        speaking.value = false
        resolve()
      }
      window.speechSynthesis.speak(utterance)
    })
  }

  function cancel() {
    if (ttsSupported) window.speechSynthesis.cancel()
    speaking.value = false
  }

  onMounted(() => {
    if (!ttsSupported) return
    // 목록이 비동기로 채워지는 브라우저(Chrome 등)를 위해 이벤트로도 갱신하고, 이미 채워져
    // 있는 브라우저(Safari 등)를 위해 한 번 즉시 시도한다.
    refreshPreferredVoice()
    window.speechSynthesis.onvoiceschanged = refreshPreferredVoice
  })

  onUnmounted(() => {
    if (!ttsSupported) return
    window.speechSynthesis.cancel()
    window.speechSynthesis.onvoiceschanged = null
  })

  return { ttsSupported, speaking, speak, cancel }
}
