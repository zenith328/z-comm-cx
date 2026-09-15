<script setup lang="ts">
import { ref, watch } from 'vue'
import { askShowhost, fetchShowhost, regenerateShowhost } from '../api/showhostApi'
import { useTts } from '../composables/useTts'
import { isQuotaExceededError } from '../utils/apiError'

interface QnaEntry {
  question: string
  answer: string
}

// 고객화면(ShowhostView)과 운영자화면(AdminProductDetailView의 쇼호스트 방송 탭)이 이 패널을
// 그대로 재사용한다. 대본을 (다시) 생성하는 건 운영자만 할 수 있어야 해서 allowRegenerate로 구분한다.
const props = withDefaults(
  defineProps<{
    productCode: string
    allowRegenerate?: boolean
  }>(),
  { allowRegenerate: false },
)

const script = ref('')
const avatarImageUrl = ref<string | null>(null)

const loading = ref(false)
const loadError = ref('')
const regenerating = ref(false)

const question = ref('')
const asking = ref(false)
const qnaError = ref('')
const qnaHistory = ref<QnaEntry[]>([])

const { ttsSupported, speaking, speak, cancel } = useTts()

async function load() {
  loading.value = true
  loadError.value = ''
  cancel()
  qnaHistory.value = []
  try {
    const showhost = await fetchShowhost(props.productCode)
    script.value = showhost.script
    avatarImageUrl.value = showhost.avatarImageUrl
  } catch (error) {
    console.error(error)
    loadError.value = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과해 지금은 방송 대본을 만들 수 없습니다. 잠시 후 다시 시도해주세요.'
      : '방송 대본을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function regenerate() {
  regenerating.value = true
  loadError.value = ''
  cancel()
  try {
    const showhost = await regenerateShowhost(props.productCode)
    script.value = showhost.script
    avatarImageUrl.value = showhost.avatarImageUrl
  } catch (error) {
    console.error(error)
    loadError.value = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과해 지금은 다시 생성할 수 없습니다. 잠시 후 다시 시도해주세요.'
      : '대본을 다시 생성하지 못했습니다.'
  } finally {
    regenerating.value = false
  }
}

function playScript() {
  if (!script.value) return
  void speak(script.value)
}

async function askQuestion() {
  const q = question.value.trim()
  if (!q) return
  asking.value = true
  qnaError.value = ''
  try {
    const result = await askShowhost(props.productCode, q)
    qnaHistory.value.push({ question: q, answer: result.answer })
    question.value = ''
    void speak(result.answer)
  } catch (error) {
    console.error(error)
    qnaError.value = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과해 지금은 답변할 수 없습니다. 잠시 후 다시 시도해주세요.'
      : '질문에 답변하지 못했습니다.'
  } finally {
    asking.value = false
  }
}

watch(() => props.productCode, load, { immediate: true })
</script>

<template>
  <div class="showhost-panel">
    <p v-if="loading" class="loading">방송을 준비하는 중...</p>
    <p v-else-if="loadError" class="error">{{ loadError }}</p>

    <template v-else>
      <section class="broadcast">
        <div class="avatar-box">
          <img v-if="avatarImageUrl" :src="avatarImageUrl" alt="AI 쇼호스트" class="avatar-image" />
          <div v-else class="avatar-placeholder">아직 등록된 쇼호스트 이미지가 없습니다.</div>
          <span v-if="speaking" class="speaking-badge">방송 중</span>
        </div>

        <div class="script-panel">
          <p class="script-text">{{ script }}</p>
          <div class="script-controls">
            <button type="button" :disabled="!ttsSupported || !script" @click="playScript">
              🔊 방송 듣기
            </button>
            <button type="button" :disabled="!speaking" @click="cancel">정지</button>
            <button
              v-if="allowRegenerate"
              type="button"
              class="regenerate"
              :disabled="regenerating"
              @click="regenerate"
            >
              {{ regenerating ? '다시 생성 중...' : '대본 다시 생성' }}
            </button>
          </div>
          <p v-if="!ttsSupported" class="tts-notice">이 브라우저는 음성 재생을 지원하지 않아 자막만 표시됩니다.</p>
        </div>
      </section>

      <section class="qna">
        <h3>쇼호스트에게 물어보기</h3>
        <form class="qna-form" @submit.prevent="askQuestion">
          <input
            v-model="question"
            type="text"
            placeholder="예: 이 상품 세탁은 어떻게 해요?"
            :disabled="asking"
          />
          <button type="submit" :disabled="asking || !question.trim()">
            {{ asking ? '답변 중...' : '질문하기' }}
          </button>
        </form>
        <p v-if="qnaError" class="error">{{ qnaError }}</p>

        <ul v-if="qnaHistory.length" class="qna-history">
          <li v-for="(entry, index) in qnaHistory" :key="index">
            <p class="q">Q. {{ entry.question }}</p>
            <p class="a">A. {{ entry.answer }}</p>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<style scoped>
.loading,
.error {
  font-size: 13px;
}
.error {
  color: #a80000;
}
.broadcast {
  display: flex;
  gap: 20px;
  padding: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  background: #0b1220;
  margin-bottom: 24px;
}
.avatar-box {
  position: relative;
  flex: 0 0 200px;
  height: 260px;
  border-radius: 8px;
  overflow: hidden;
  background: #1c2434;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-placeholder {
  padding: 12px;
  text-align: center;
  font-size: 12px;
  color: #8a93a6;
}
.speaking-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 3px 10px;
  border-radius: 999px;
  background: #e63946;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}
.script-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
.script-text {
  margin: 0;
  flex: 1;
  padding: 14px 16px;
  border-radius: 8px;
  background: #ffffff;
  color: #222;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.script-controls {
  display: flex;
  gap: 8px;
}
.script-controls button {
  padding: 8px 14px;
  border: none;
  border-radius: 6px;
  background: #0056b3;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.script-controls button:disabled {
  opacity: 0.5;
  cursor: default;
}
.script-controls button.regenerate {
  margin-left: auto;
  background: #444;
}
.tts-notice {
  margin: 0;
  font-size: 12px;
  color: #b0b8c8;
}
.qna h3 {
  margin: 0 0 10px;
  font-size: 16px;
}
.qna-form {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.qna-form input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 14px;
}
.qna-form button {
  padding: 10px 16px;
  border: none;
  border-radius: 6px;
  background: #0056b3;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.qna-form button:disabled {
  opacity: 0.6;
  cursor: default;
}
.qna-history {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.qna-history li {
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fafbfc;
}
.qna-history .q {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 600;
  color: #333;
}
.qna-history .a {
  margin: 0;
  font-size: 13px;
  color: #444;
  line-height: 1.5;
}

@media (max-width: 700px) {
  .broadcast {
    flex-direction: column;
  }
  .avatar-box {
    flex: none;
    width: 100%;
    height: 200px;
  }
}
</style>
