<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import '../styles/admin.css'
import { escalateTicket, listTickets, resolveTicket, startTicketProgress } from '../api/tickets'
import type { TicketChannel, TicketResponse, TicketStatus } from '../api/cs-types'
import { maskName, maskPhone } from '../utils/mask'
import { formatDateTime } from '../utils/format'
import { renderMarkdownLite } from '../utils/markdown'

const tickets = ref<TicketResponse[]>([])
const loading = ref(false)
const errorMessage = ref('')
const resolvingId = ref<number | null>(null)
const resolutionDraft = ref('')
const transcriptTicket = ref<TicketResponse | null>(null)

interface TranscriptMessage {
  type: 'customer' | 'ai' | 'tool'
  text: string
}

// 예전에 저장된 티켓은 tool 이름이 영문 그대로("cancel_order" 등) 저장돼 있을 수 있어 하위 호환으로 번역해준다.
const TOOL_LABELS: Record<string, string> = {
  get_order_details: '주문 조회',
  get_my_orders: '내 주문 목록 조회',
  cancel_order: '주문 취소',
  change_shipping_address: '배송지 변경',
  request_return: '반품 접수',
  escalate_to_human: '상담원 이관',
}

const parsedTranscript = computed<TranscriptMessage[]>(() => {
  const raw = transcriptTicket.value?.chatTranscript ?? ''
  const messages: TranscriptMessage[] = []

  for (const line of raw.split('\n')) {
    const toolMatch = line.match(/^AI \((처리|도구 호출)\):\s*(.*)$/)
    if (line.startsWith('고객:')) {
      messages.push({ type: 'customer', text: line.replace(/^고객:\s*/, '') })
    } else if (toolMatch) {
      const action = toolMatch[2]
      messages.push({ type: 'tool', text: `🔧 ${TOOL_LABELS[action] ?? action}` })
    } else if (line.startsWith('AI:')) {
      messages.push({ type: 'ai', text: line.replace(/^AI:\s*/, '') })
    } else if (messages.length > 0) {
      // 이전 메시지(주로 AI 답변)가 여러 줄로 이어지는 경우 — 새 말풍선을 만들지 않고 이어붙인다.
      messages[messages.length - 1].text += `\n${line}`
    }
  }

  return messages.map((message) => ({ ...message, text: message.text.trim() })).filter((message) => message.text.length > 0)
})

const STATUS_FILTER_OPTIONS: { value: TicketStatus; label: string }[] = [
  { value: 'IN_PROGRESS', label: '처리중' },
  { value: 'ESCALATED', label: '상담원 이관' },
  { value: 'CLOSED', label: '처리완료' },
]

const CHANNEL_FILTER_OPTIONS: { value: TicketChannel; label: string }[] = [
  { value: 'AI', label: 'AI' },
  { value: 'HUMAN', label: 'HUMAN' },
]

// 최초 진입 시 처리완료(CLOSED)는 제외하고 처리중/상담원 이관만 보여준다.
const statusFilter = ref<TicketStatus[]>(['IN_PROGRESS', 'ESCALATED'])
const channelFilter = ref<TicketChannel[]>([])
const searchQuery = ref('')
const sortOrder = ref<'asc' | 'desc'>('asc')

const filteredTickets = computed(() => {
  const query = searchQuery.value.trim()
  return tickets.value.filter((ticket) => {
    if (statusFilter.value.length > 0 && !statusFilter.value.includes(ticket.status)) return false
    if (channelFilter.value.length > 0 && !channelFilter.value.includes(ticket.channel)) return false
    if (query) {
      const nameMatch = ticket.customerName?.includes(query) ?? false
      const phoneMatch = ticket.customerPhone?.includes(query) ?? false
      if (!nameMatch && !phoneMatch) return false
    }
    return true
  })
})

const sortedTickets = computed(() => {
  const list = [...filteredTickets.value]
  list.sort((a, b) => {
    const diff = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    return sortOrder.value === 'asc' ? diff : -diff
  })
  return list
})

interface TicketGroup {
  key: string
  tickets: TicketResponse[]
}

// 같은 주문번호의 티켓을 붙여서 보여준다 (주문번호가 없는 티켓은 각자 별도 그룹).
// 그룹 위치는 정렬 순서상 그 그룹의 가장 먼저 나오는 티켓 기준으로 고정한다.
const ticketGroups = computed<TicketGroup[]>(() => {
  const groups = new Map<string, TicketResponse[]>()
  const order: string[] = []
  for (const ticket of sortedTickets.value) {
    const key = ticket.orderNo ?? `__ticket_${ticket.id}`
    if (!groups.has(key)) {
      groups.set(key, [])
      order.push(key)
    }
    groups.get(key)!.push(ticket)
  }
  return order.map((key) => ({ key, tickets: groups.get(key)! }))
})

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    tickets.value = await listTickets()
  } catch {
    errorMessage.value = '티켓 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function startResolve(ticket: TicketResponse) {
  resolvingId.value = ticket.id
  resolutionDraft.value = ''
}

async function submitResolve(ticket: TicketResponse) {
  if (!resolutionDraft.value.trim()) return
  await resolveTicket(ticket.id, resolutionDraft.value.trim())
  resolvingId.value = null
  await load()
}

async function escalate(ticket: TicketResponse) {
  await escalateTicket(ticket.id)
  await load()
}

async function startProgress(ticket: TicketResponse) {
  await startTicketProgress(ticket.id)
  await load()
}

function openTranscript(ticket: TicketResponse) {
  transcriptTicket.value = ticket
}

function statusLabel(status: TicketStatus) {
  switch (status) {
    case 'OPEN':
      return '접수'
    case 'IN_PROGRESS':
      return '처리중'
    case 'ESCALATED':
      return '상담원 이관'
    case 'CLOSED':
      return '처리완료'
  }
}

function statusTone(status: TicketStatus): string {
  switch (status) {
    case 'IN_PROGRESS':
      return 'info'
    case 'ESCALATED':
      return 'warning'
    case 'CLOSED':
      return 'success'
    default:
      return ''
  }
}

onMounted(load)
</script>

<template>
  <section class="admin-page">
    <h2 class="admin-title">CS 티켓 목록</h2>

    <div class="admin-toolbar">
      <button type="button" @click="load" :disabled="loading">새로고침</button>

      <div class="admin-filter">
        <span class="admin-filter-label">상태</span>
        <label v-for="option in STATUS_FILTER_OPTIONS" :key="option.value" class="admin-checkbox">
          <input type="checkbox" :value="option.value" v-model="statusFilter" />
          {{ option.label }}
        </label>
      </div>

      <div class="admin-filter">
        <span class="admin-filter-label">채널</span>
        <label v-for="option in CHANNEL_FILTER_OPTIONS" :key="option.value" class="admin-checkbox">
          <input type="checkbox" :value="option.value" v-model="channelFilter" />
          {{ option.label }}
        </label>
      </div>

      <label class="admin-filter admin-filter-plain">
        <span class="admin-filter-label">검색</span>
        <input v-model="searchQuery" type="text" placeholder="이름 또는 전화번호" />
      </label>
    </div>

    <p v-if="errorMessage" class="admin-error">{{ errorMessage }}</p>
    <p v-else-if="loading">불러오는 중...</p>
    <p v-else-if="tickets.length === 0">티켓이 없습니다.</p>
    <p v-else-if="sortedTickets.length === 0">조건에 맞는 티켓이 없습니다.</p>

    <table v-else class="admin-table ticket-table">
      <thead>
        <tr>
          <th>티켓번호</th>
          <th>주문번호</th>
          <th>고객정보</th>
          <th>유형</th>
          <th>상태</th>
          <th>채널</th>
          <th>요약</th>
          <th class="sortable-header">
            <span>접수일시</span>
            <button
              type="button"
              class="sort-button"
              :title="sortOrder === 'asc' ? '최신순으로 정렬' : '오래된순으로 정렬'"
              @click="sortOrder = sortOrder === 'asc' ? 'desc' : 'asc'"
            >
              {{ sortOrder === 'asc' ? '↓' : '↑' }}
            </button>
          </th>
          <th>액션</th>
        </tr>
      </thead>
      <tbody>
        <template v-for="(group, groupIndex) in ticketGroups" :key="group.key">
          <tr v-for="ticket in group.tickets" :key="ticket.id" :class="{ 'row-shade': groupIndex % 2 === 1 }">
            <td>{{ ticket.ticketNo }}</td>
            <td>{{ ticket.orderNo ?? '-' }}</td>
            <td>
              {{ maskName(ticket.customerName) }}<br />
              <span class="admin-muted">{{ maskPhone(ticket.customerPhone) }}</span>
            </td>
            <td>{{ ticket.category }}</td>
            <td><span :class="['admin-status', statusTone(ticket.status)]">{{ statusLabel(ticket.status) }}</span></td>
            <td>{{ ticket.channel }}</td>
            <td class="summary">
              {{ ticket.summary }}
              <div v-if="ticket.resolution" class="resolution">처리결과: {{ ticket.resolution }}</div>
              <button v-if="ticket.chatTranscript" type="button" class="link-button" @click="openTranscript(ticket)">
                대화 보기
              </button>
            </td>
            <td>{{ formatDateTime(ticket.createdAt) }}</td>
            <td>
              <div v-if="ticket.status !== 'CLOSED'" class="admin-actions">
                <button
                  v-if="ticket.status === 'OPEN' || ticket.status === 'ESCALATED'"
                  type="button"
                  @click="startProgress(ticket)"
                >
                  처리중
                </button>
                <button v-if="ticket.channel !== 'HUMAN'" type="button" @click="escalate(ticket)">이관</button>

                <template v-if="resolvingId === ticket.id">
                  <input v-model="resolutionDraft" type="text" placeholder="처리 내용" />
                  <button type="button" @click="submitResolve(ticket)">확인</button>
                  <button type="button" @click="resolvingId = null">취소</button>
                </template>
                <button v-else type="button" @click="startResolve(ticket)">처리완료</button>
              </div>
            </td>
          </tr>
        </template>
      </tbody>
    </table>

    <div v-if="transcriptTicket" class="modal-overlay" @click.self="transcriptTicket = null">
      <div class="transcript-modal">
        <div class="modal-header">
          <h3>{{ transcriptTicket.ticketNo }} 대화 내용</h3>
          <button type="button" class="close-button" @click="transcriptTicket = null">닫기</button>
        </div>
        <div class="transcript-messages">
          <div
            v-for="(message, i) in parsedTranscript"
            :key="i"
            :class="['bubble', message.type]"
            v-html="renderMarkdownLite(message.text)"
          ></div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ticket-table th:nth-child(8),
.ticket-table td:nth-child(8) {
  width: 100px;
}
.ticket-table th:nth-child(9),
.ticket-table td:nth-child(9) {
  width: 190px;
}
.summary {
  max-width: 320px;
  white-space: pre-wrap;
}
.row-shade td {
  background: #fbf3ea;
}
.resolution {
  margin-top: 4px;
  color: #555;
  font-size: 12px;
}
.link-button {
  display: inline-block;
  margin-top: 6px;
  padding: 0;
  border: none;
  background: none;
  color: #0056b3;
  font-size: 12px;
  text-decoration: underline;
  cursor: pointer;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
}
.transcript-modal {
  width: 480px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #eee;
}
.modal-header h3 {
  margin: 0;
  font-size: 15px;
}
.close-button {
  padding: 4px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
}
.transcript-messages {
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
.bubble.customer {
  align-self: flex-end;
  background: #0056b3;
  color: #fff;
}
.bubble.ai {
  align-self: flex-start;
  background: #fff;
  border: 1px solid #e0e0e0;
}
.bubble.tool {
  align-self: center;
  max-width: 90%;
  padding: 2px 8px;
  color: #999;
  font-size: 12px;
  font-style: italic;
  text-align: center;
}
</style>
