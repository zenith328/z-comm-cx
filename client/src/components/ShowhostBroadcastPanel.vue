<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  askShowhost,
  discountedPrice,
  fetchShowhost,
  fetchShowhostDiscount,
  issueShowhostOffer,
  regenerateShowhost,
} from '../api/showhostApi'
import { useTts } from '../composables/useTts'
import { session } from '../stores/session'
import type { ShowhostDiscountSetting, ShowhostOffer } from '../types/showhost'
import { isQuotaExceededError } from '../utils/apiError'

interface QnaEntry {
  question: string
  answer: string
}

// 고객화면(ShowhostView)과 운영자화면(AdminProductDetailView의 쇼호스트 방송 탭)이 이 패널을
// 그대로 재사용한다. 대본을 (다시) 생성하는 건 운영자만 할 수 있어야 해서 allowRegenerate로 구분한다.
// 방송 특가는 고객화면에서만 발급(issueOffer)한다 — 운영자가 미리보기로 들어올 때마다 특가가
// 발급되면 안 되므로, 운영자 탭에서는 안내 멘트만 미리 보여준다.
const props = withDefaults(
  defineProps<{
    productCode: string
    productId?: number | null
    price?: number | null
    allowRegenerate?: boolean
    issueOffer?: boolean
  }>(),
  { productId: null, price: null, allowRegenerate: false, issueOffer: false },
)

const router = useRouter()

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

const discount = ref<ShowhostDiscountSetting | null>(null)
const offer = ref<ShowhostOffer | null>(null)
const remainingSeconds = ref(0)
let offerDeadline = 0
let countdownTimer: ReturnType<typeof setInterval> | null = null

const offerActive = computed(() => !!offer.value && !offer.value.used && remainingSeconds.value > 0)
const offerDiscountedPrice = computed(() =>
  offer.value && props.price != null ? discountedPrice(props.price, offer.value.discountRate) : null,
)
const remainingText = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60)
  const seconds = remainingSeconds.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

// AI가 할인율을 틀리게 말하지 않도록, 특가 안내는 대본에 섞지 않고 고정 문구로 뒤에 붙인다.
// 고객은 실제로 받은 특가가 유효할 때만, 운영자 미리보기는 특가가 켜져 있으면 설정값으로 보여준다.
const discountAnnouncement = computed(() => {
  const setting = discount.value
  if (!setting?.enabled) return ''
  if (props.issueOffer && !offerActive.value) return ''
  const rate = props.issueOffer && offer.value ? offer.value.discountRate : setting.discountRate
  return (
    `지금 이 방송을 보고 계신 분께만, ${setting.validMinutes}분 동안 ${rate}% 할인된 가격으로 드려요. ` +
    '화면 아래 방송 특가로 구매 버튼을 눌러주세요.'
  )
})

function stopCountdown() {
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = null
}

function tick() {
  remainingSeconds.value = Math.max(0, Math.round((offerDeadline - Date.now()) / 1000))
  if (remainingSeconds.value === 0) stopCountdown()
}

// 서버가 준 남은 초를 기준으로 브라우저에서 마감 시각을 잡는다(서버/브라우저 시계 차이 무시).
function startCountdown(received: ShowhostOffer) {
  stopCountdown()
  offerDeadline = Date.now() + received.remainingSeconds * 1000
  tick()
  if (remainingSeconds.value > 0) countdownTimer = setInterval(tick, 1000)
}

async function loadOffer() {
  offer.value = null
  stopCountdown()
  remainingSeconds.value = 0
  if (!props.issueOffer || !discount.value?.enabled || !session.current) return
  try {
    const received = await issueShowhostOffer(props.productCode, session.current.name, session.current.phone)
    offer.value = received
    if (received && !received.used) startCountdown(received)
  } catch (error) {
    // 특가 발급 실패는 방송 시청 자체를 막을 이유가 아니라서 조용히 넘어간다.
    console.error(error)
  }
}

function buyWithOffer() {
  if (!offer.value || props.productId == null) return
  cancel()
  router.push({
    path: '/orders/new',
    query: { productId: props.productId, quantity: 1, showhostOfferId: offer.value.id },
  })
}

onUnmounted(stopCountdown)

async function load() {
  loading.value = true
  loadError.value = ''
  cancel()
  qnaHistory.value = []
  try {
    const [showhost, discountSetting] = await Promise.all([
      fetchShowhost(props.productCode),
      fetchShowhostDiscount(props.productCode),
    ])
    script.value = showhost.script
    avatarImageUrl.value = showhost.avatarImageUrl
    discount.value = discountSetting
    await loadOffer()
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
  void speak(discountAnnouncement.value ? `${script.value} ${discountAnnouncement.value}` : script.value)
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
          <p v-if="discountAnnouncement" class="discount-announcement">📣 {{ discountAnnouncement }}</p>
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

      <section v-if="issueOffer && offer" class="offer" :class="{ inactive: !offerActive }">
        <template v-if="offerActive">
          <div class="offer-info">
            <span class="offer-badge">방송 특가 {{ offer.discountRate }}%</span>
            <span v-if="price != null && offerDiscountedPrice != null" class="offer-price">
              <s>{{ price.toLocaleString() }}원</s>
              <strong>{{ offerDiscountedPrice.toLocaleString() }}원</strong>
            </span>
            <span class="offer-timer">남은 시간 {{ remainingText }}</span>
          </div>
          <button type="button" class="offer-buy" :disabled="productId == null" @click="buyWithOffer">
            방송 특가로 구매
          </button>
        </template>
        <p v-else-if="offer.used" class="offer-ended">오늘 이 상품의 방송 특가는 이미 사용하셨습니다.</p>
        <p v-else class="offer-ended">방송 특가 시간이 끝났습니다. 방송 특가는 하루 1번만 받을 수 있어요.</p>
      </section>
      <p v-else-if="!issueOffer && discount?.enabled" class="offer-preview-notice">
        고객 화면에서는 방송에 들어온 순간부터 {{ discount.validMinutes }}분 타이머와 "방송 특가로 구매" 버튼이
        표시됩니다. (운영자 미리보기에서는 특가가 발급되지 않습니다)
      </p>

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
.discount-announcement {
  margin: 0;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fff4d6;
  color: #7a4b00;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
}
.offer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 18px;
  margin: -12px 0 24px;
  border: 2px solid #e63946;
  border-radius: 10px;
  background: #fff5f5;
}
.offer.inactive {
  border-color: #ddd;
  background: #f5f5f5;
}
.offer-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.offer-badge {
  padding: 4px 10px;
  border-radius: 999px;
  background: #e63946;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}
.offer-price s {
  margin-right: 6px;
  font-size: 13px;
  color: #999;
}
.offer-price strong {
  font-size: 17px;
  color: #e63946;
}
.offer-timer {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  font-variant-numeric: tabular-nums;
}
.offer-buy {
  padding: 10px 18px;
  border: none;
  border-radius: 6px;
  background: #e63946;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}
.offer-buy:disabled {
  opacity: 0.5;
  cursor: default;
}
.offer-ended {
  margin: 0;
  font-size: 13px;
  color: #666;
}
.offer-preview-notice {
  margin: -12px 0 24px;
  font-size: 12px;
  color: #7a4b00;
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
