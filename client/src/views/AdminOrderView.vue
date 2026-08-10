<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import '../styles/admin.css'
import { deliverOrder, listOrders, shipOrder } from '../api/orders'
import type { OrderResponse, OrderStatus } from '../api/cs-types'
import { maskName, maskPhone } from '../utils/mask'
import { formatDateTime } from '../utils/format'

const orders = ref<OrderResponse[]>([])
const loading = ref(false)
const errorMessage = ref('')
const actionError = ref('')
const actingId = ref<number | null>(null)

// PREPARING/RETURNED는 현재 이 프로그램에서 실제로 전환되는 코드 경로가 없어 필터 옵션에서 제외했다.
const STATUS_FILTER_OPTIONS: { value: OrderStatus; label: string }[] = [
  { value: 'PAID', label: '결제완료' },
  { value: 'SHIPPING', label: '배송중' },
  { value: 'DELIVERED', label: '배송완료' },
  { value: 'CANCELLED', label: '취소됨' },
  { value: 'RETURN_REQUESTED', label: '반품접수' },
]

const statusFilter = ref<OrderStatus[]>([])
const searchQuery = ref('')
// 최초 진입 시 주문일시 내림차순(최신순)으로 보여준다.
const sortOrder = ref<'asc' | 'desc'>('desc')

const filteredOrders = computed(() => {
  const query = searchQuery.value.trim()
  return orders.value.filter((order) => {
    if (statusFilter.value.length > 0 && !statusFilter.value.includes(order.status)) return false
    if (query) {
      const nameMatch = order.customerName.includes(query)
      const phoneMatch = order.customerPhone.includes(query)
      if (!nameMatch && !phoneMatch) return false
    }
    return true
  })
})

const sortedOrders = computed(() => {
  const list = [...filteredOrders.value]
  list.sort((a, b) => {
    const diff = new Date(a.orderedAt).getTime() - new Date(b.orderedAt).getTime()
    return sortOrder.value === 'asc' ? diff : -diff
  })
  return list
})

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    orders.value = await listOrders()
  } catch {
    errorMessage.value = '주문 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function replaceOrder(updated: OrderResponse) {
  const index = orders.value.findIndex((o) => o.id === updated.id)
  if (index !== -1) orders.value[index] = updated
}

async function handleShip(order: OrderResponse) {
  actingId.value = order.id
  actionError.value = ''
  try {
    replaceOrder(await shipOrder(order.id))
  } catch {
    actionError.value = `주문 ${order.orderNo} 배송처리에 실패했습니다.`
  } finally {
    actingId.value = null
  }
}

async function handleDeliver(order: OrderResponse) {
  actingId.value = order.id
  actionError.value = ''
  try {
    replaceOrder(await deliverOrder(order.id))
  } catch {
    actionError.value = `주문 ${order.orderNo} 배송완료 처리에 실패했습니다.`
  } finally {
    actingId.value = null
  }
}

function statusLabel(status: OrderStatus) {
  switch (status) {
    case 'PAID':
      return '결제완료'
    case 'PREPARING':
      return '상품준비중'
    case 'SHIPPING':
      return '배송중'
    case 'DELIVERED':
      return '배송완료'
    case 'CANCELLED':
      return '취소됨'
    case 'RETURN_REQUESTED':
      return '반품접수'
    case 'RETURNED':
      return '반품완료'
  }
}

function statusTone(status: OrderStatus): string {
  switch (status) {
    case 'DELIVERED':
      return 'success'
    case 'RETURN_REQUESTED':
      return 'warning'
    case 'CANCELLED':
    case 'RETURNED':
      return 'muted'
    default:
      return ''
  }
}

function itemsSummary(order: OrderResponse) {
  return order.items.map((item) => `${item.productName} x${item.quantity}`).join(', ')
}

function orderTotal(order: OrderResponse): string {
  const total = order.items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0)
  return `${total.toLocaleString()}원`
}

function addressSummary(order: OrderResponse) {
  return [order.zipcode, order.address1, order.address2].filter(Boolean).join(' ')
}

onMounted(load)
</script>

<template>
  <section class="admin-page">
    <h2 class="admin-title">주문 목록</h2>

    <div class="admin-toolbar">
      <button type="button" @click="load" :disabled="loading">새로고침</button>

      <div class="admin-filter">
        <span class="admin-filter-label">상태</span>
        <label v-for="option in STATUS_FILTER_OPTIONS" :key="option.value" class="admin-checkbox">
          <input type="checkbox" :value="option.value" v-model="statusFilter" />
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
    <p v-else-if="orders.length === 0">주문 내역이 없습니다.</p>
    <p v-else-if="sortedOrders.length === 0">조건에 맞는 주문이 없습니다.</p>

    <template v-else>
      <p v-if="actionError" class="admin-error">{{ actionError }}</p>

      <table class="admin-table">
        <thead>
          <tr>
            <th>주문번호</th>
            <th>고객정보</th>
            <th>주문상품</th>
            <th class="order-price">가격</th>
            <th>배송지</th>
            <th>상태</th>
            <th class="sortable-header">
              <span>주문일시</span>
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
          <tr v-for="order in sortedOrders" :key="order.id">
            <td>{{ order.orderNo }}</td>
            <td>
              {{ maskName(order.customerName) }}<br />
              <span class="admin-muted">{{ maskPhone(order.customerPhone) }}</span>
            </td>
            <td>{{ itemsSummary(order) }}</td>
            <td class="order-price">{{ orderTotal(order) }}</td>
            <td>
              {{ maskName(order.recipientName) }}<br />
              <span class="admin-muted">{{ addressSummary(order) }}</span>
            </td>
            <td><span :class="['admin-status', statusTone(order.status)]">{{ statusLabel(order.status) }}</span></td>
            <td>{{ formatDateTime(order.orderedAt) }}</td>
            <td>
              <div class="admin-actions">
                <button
                  v-if="order.status === 'PAID' || order.status === 'PREPARING'"
                  type="button"
                  :disabled="actingId === order.id"
                  @click="handleShip(order)"
                >
                  배송처리
                </button>
                <button
                  v-else-if="order.status === 'SHIPPING'"
                  type="button"
                  :disabled="actingId === order.id"
                  @click="handleDeliver(order)"
                >
                  배송완료
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </template>
  </section>
</template>

<style scoped>
.order-price {
  white-space: nowrap;
}
</style>
