<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import '../styles/admin.css'
import { clearSummaryCache, fetchDbUsage, purgeTicketTranscripts } from '../api/dbUsage'
import type { DbUsageResponse } from '../api/dbUsage'
import { fetchGeminiUsage } from '../api/geminiUsage'
import type { DailyUsageResponse, GeminiApiUsageResponse } from '../api/geminiUsage'
import { formatBytes } from '../utils/format'

const usage = ref<DbUsageResponse | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const usedPercent = computed(() => {
  if (!usage.value || usage.value.limitBytes === 0) return 0
  return Math.min(100, (usage.value.totalBytes / usage.value.limitBytes) * 100)
})

// 80% 넘으면 주의, 95% 넘으면 위험 — 그 전까지는 정상.
const usageTone = computed(() => {
  if (usedPercent.value >= 95) return 'danger'
  if (usedPercent.value >= 80) return 'warning'
  return 'ok'
})

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    usage.value = await fetchDbUsage()
  } catch {
    errorMessage.value = 'DB 용량 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const cacheClearing = ref(false)
const cacheResultMessage = ref('')

async function onClearSummaryCache() {
  if (!confirm('AI 리뷰 요약 캐시를 전부 삭제할까요? (다음 요청부터 다시 AI를 호출해 생성합니다)')) return
  cacheClearing.value = true
  cacheResultMessage.value = ''
  try {
    const result = await clearSummaryCache()
    cacheResultMessage.value = `${result.deletedCount}건 삭제했습니다.`
    await load()
  } catch {
    cacheResultMessage.value = '삭제에 실패했습니다.'
  } finally {
    cacheClearing.value = false
  }
}

const olderThanDays = ref(90)
const transcriptPurging = ref(false)
const transcriptResultMessage = ref('')

async function onPurgeTranscripts() {
  if (olderThanDays.value < 1) return
  if (
    !confirm(
      `처리완료된 지 ${olderThanDays.value}일이 지난 CS 티켓의 대화록만 삭제합니다 (티켓 자체는 남습니다). 계속할까요?`,
    )
  )
    return
  transcriptPurging.value = true
  transcriptResultMessage.value = ''
  try {
    const result = await purgeTicketTranscripts(olderThanDays.value)
    transcriptResultMessage.value = `${result.clearedCount}건의 대화록을 삭제했습니다.`
    await load()
  } catch {
    transcriptResultMessage.value = '삭제에 실패했습니다.'
  } finally {
    transcriptPurging.value = false
  }
}

const geminiUsage = ref<GeminiApiUsageResponse | null>(null)
const geminiLoading = ref(false)
const geminiError = ref('')

const geminiUsedPercent = computed(() => {
  if (!geminiUsage.value || geminiUsage.value.limitPerDay === 0) return 0
  return Math.min(100, (geminiUsage.value.today.requestCount / geminiUsage.value.limitPerDay) * 100)
})

const geminiUsageTone = computed(() => {
  if (geminiUsedPercent.value >= 95) return 'danger'
  if (geminiUsedPercent.value >= 80) return 'warning'
  return 'ok'
})

// koreaRangeStart/End는 "YYYY-MM-DDTHH:mm:ss" 형식(LocalDateTime) — "MM-DD HH:mm"만 뽑아서 보여준다.
function formatKoreaTime(value: string): string {
  return value.replace('T', ' ').slice(5, 16)
}

function formatDateWithZone(day: DailyUsageResponse): string {
  return `${day.date} (PT) (한국시간 ${formatKoreaTime(day.koreaRangeStart)} ~ ${formatKoreaTime(day.koreaRangeEnd)})`
}

async function loadGeminiUsage() {
  geminiLoading.value = true
  geminiError.value = ''
  try {
    geminiUsage.value = await fetchGeminiUsage()
  } catch {
    geminiError.value = 'AI API 사용량 정보를 불러오지 못했습니다.'
  } finally {
    geminiLoading.value = false
  }
}

onMounted(load)
onMounted(loadGeminiUsage)

type SystemTab = 'db' | 'gemini'
const activeTab = ref<SystemTab>('gemini')
</script>

<template>
  <section class="admin-page">
    <!-- 앞으로 이 화면에 다른 시스템 관리 기능이 추가되면, 탭을 하나 더 추가하면 된다. -->
    <div class="system-tabs">
      <button
        type="button"
        class="system-tab"
        :class="{ active: activeTab === 'gemini' }"
        @click="activeTab = 'gemini'"
      >
        AI API 관리 (제미나이 API 사용량)
      </button>
      <button type="button" class="system-tab" :class="{ active: activeTab === 'db' }" @click="activeTab = 'db'">
        DB 관리 (Supabase 용량)
      </button>
    </div>

    <section v-show="activeTab === 'gemini'" class="tab-panel">
      <p v-if="geminiError" class="admin-error">{{ geminiError }}</p>
      <p v-else-if="geminiLoading && !geminiUsage">불러오는 중...</p>

      <template v-else-if="geminiUsage">
        <section class="usage-box">
          <div class="usage-header">
            <h3>오늘 요청 수 (RPD) — {{ geminiUsage.tier }} 등급</h3>
            <button type="button" @click="loadGeminiUsage" :disabled="geminiLoading">새로고침</button>
          </div>
          <div class="usage-bar-track">
            <div class="usage-bar-fill" :class="geminiUsageTone" :style="{ width: `${geminiUsedPercent}%` }"></div>
          </div>
          <p class="usage-date">{{ formatDateWithZone(geminiUsage.today) }}</p>
          <p class="usage-summary">
            {{ geminiUsage.today.requestCount }} / {{ geminiUsage.limitPerDay }}회 사용 중
            ({{ geminiUsedPercent.toFixed(1) }}%), 토큰 {{ geminiUsage.today.tokenCount.toLocaleString() }} 사용
            — {{ geminiUsage.tier }} 등급 한도(RPM {{ geminiUsage.rpmLimit }}, TPM
            {{ geminiUsage.tpmLimit.toLocaleString() }}, RPD {{ geminiUsage.limitPerDay }}) 중 RPD만 제어하고
            토큰은 참고용으로만 누적합니다.
          </p>

          <table class="admin-table">
            <thead>
              <tr>
                <th>날짜</th>
                <th>요청 수</th>
                <th>토큰 수</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="geminiUsage.recent.length === 0">
                <td colspan="3">기록이 없습니다.</td>
              </tr>
              <tr v-for="day in geminiUsage.recent" :key="day.date">
                <td>{{ formatDateWithZone(day) }}</td>
                <td>{{ day.requestCount.toLocaleString() }}</td>
                <td>{{ day.tokenCount.toLocaleString() }}</td>
              </tr>
            </tbody>
          </table>
        </section>
      </template>
    </section>

    <section v-show="activeTab === 'db'" class="tab-panel">
      <p v-if="errorMessage" class="admin-error">{{ errorMessage }}</p>
      <p v-else-if="loading && !usage">불러오는 중...</p>

      <template v-else-if="usage">
        <section class="usage-box">
          <div class="usage-header">
            <h3>DB 사용량</h3>
            <button type="button" @click="load" :disabled="loading">새로고침</button>
          </div>
          <div class="usage-bar-track">
            <div class="usage-bar-fill" :class="usageTone" :style="{ width: `${usedPercent}%` }"></div>
          </div>
          <p class="usage-summary">
            {{ formatBytes(usage.totalBytes) }} / {{ formatBytes(usage.limitBytes) }} 사용 중
            ({{ usedPercent.toFixed(1) }}%)
          </p>

          <table class="admin-table">
            <thead>
              <tr>
                <th>테이블</th>
                <th>용량</th>
                <th>행 수(추정)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="table in usage.tables" :key="table.tableName">
                <td>{{ table.tableName }}</td>
                <td>{{ formatBytes(table.bytes) }}</td>
                <td>{{ table.rowEstimate.toLocaleString() }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="cleanup-box">
          <h3>정리하기</h3>

          <div class="cleanup-item">
            <div class="cleanup-item-text">
              <strong>AI 리뷰 요약 캐시 삭제</strong>
              <p>언제든 다시 생성 가능한 캐시라 지워도 서비스에 영향이 없습니다. 다음 요청부터 AI를 다시 호출합니다.</p>
            </div>
            <div class="cleanup-item-action">
              <button type="button" :disabled="cacheClearing" @click="onClearSummaryCache">
                {{ cacheClearing ? '삭제 중...' : '캐시 삭제' }}
              </button>
              <p v-if="cacheResultMessage" class="result-message">{{ cacheResultMessage }}</p>
            </div>
          </div>

          <div class="cleanup-item">
            <div class="cleanup-item-text">
              <strong>오래된 CS 티켓 대화록 삭제</strong>
              <p>처리완료(CLOSED)된 지 아래 일수가 지난 티켓의 원문 대화록만 지웁니다. 티켓 자체(요약/처리결과/일시)는 남습니다.</p>
            </div>
            <div class="cleanup-item-action">
              <label class="days-input">
                <input v-model.number="olderThanDays" type="number" min="1" />
                일 이전
              </label>
              <button type="button" :disabled="transcriptPurging" @click="onPurgeTranscripts">
                {{ transcriptPurging ? '삭제 중...' : '대화록 삭제' }}
              </button>
              <p v-if="transcriptResultMessage" class="result-message">{{ transcriptResultMessage }}</p>
            </div>
          </div>
        </section>
      </template>
    </section>
  </section>
</template>

<style scoped>
.system-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e0e0e0;
}
.system-tab {
  padding: 10px 18px;
  border: none;
  background: none;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.system-tab:hover {
  color: #222;
}
.system-tab.active {
  color: #0056b3;
  font-weight: 600;
  border-bottom-color: #0056b3;
}
.tab-panel {
  padding-top: 4px;
}
.usage-box,
.cleanup-box {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 20px;
  background: #fafafa;
}
.usage-box:last-child,
.cleanup-box:last-child {
  margin-bottom: 0;
}
.usage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.usage-header h3,
.cleanup-box h3 {
  margin: 0;
  font-size: 15px;
}
.usage-header button {
  padding: 5px 12px;
  font-size: 12px;
  cursor: pointer;
}
.usage-bar-track {
  width: 100%;
  height: 10px;
  border-radius: 999px;
  background: #e6e6e6;
  overflow: hidden;
}
.usage-bar-fill {
  height: 100%;
  border-radius: 999px;
  background: #0056b3;
  transition: width 0.2s ease;
}
.usage-bar-fill.warning {
  background: #d68a00;
}
.usage-bar-fill.danger {
  background: #a80000;
}
.usage-date {
  margin: 8px 0 0;
  font-size: 12px;
  color: #888;
}
.usage-summary {
  margin: 8px 0 16px;
  font-size: 13px;
  color: #555;
}
.cleanup-box {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.cleanup-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-top: 14px;
  border-top: 1px solid #eee;
}
.cleanup-box h3 + .cleanup-item {
  padding-top: 0;
  border-top: none;
}
.cleanup-item-text {
  flex: 1;
}
.cleanup-item-text strong {
  display: block;
  font-size: 13px;
  margin-bottom: 4px;
}
.cleanup-item-text p {
  margin: 0;
  font-size: 12px;
  color: #777;
  line-height: 1.5;
}
.cleanup-item-action {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.cleanup-item-action button {
  padding: 6px 14px;
  font-size: 13px;
  border: 1px solid #0056b3;
  border-radius: 4px;
  background: #fff;
  color: #0056b3;
  cursor: pointer;
  white-space: nowrap;
}
.cleanup-item-action button:disabled {
  border-color: #ccc;
  color: #ccc;
  cursor: not-allowed;
}
.days-input {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #555;
}
.days-input input {
  width: 60px;
  padding: 4px 6px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.result-message {
  width: 100%;
  margin: 0;
  font-size: 12px;
  color: #0056b3;
  text-align: right;
}
</style>
