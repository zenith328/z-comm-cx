<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listOrders } from '../api/orders'
import type { OrderResponse, OrderStatus } from '../api/cs-types'
import { session } from '../stores/session'

const router = useRouter()
const orders = ref<OrderResponse[]>([])
const loading = ref(false)
const errorMessage = ref('')

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const all = await listOrders()
    orders.value = all.filter((order) => order.customerPhone === session.current?.phone)
  } catch {
    errorMessage.value = '주문 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
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

function itemsSummary(order: OrderResponse) {
  return order.items.map((item) => `${item.productName} x${item.quantity}`).join(', ')
}

function orderTotal(order: OrderResponse): string {
  const total = order.items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0)
  return `${total.toLocaleString()}원`
}

function shippingAddress(order: OrderResponse): string {
  const zip = order.zipcode ? `[${order.zipcode}] ` : ''
  const addr2 = order.address2 ? ` ${order.address2}` : ''
  return `${zip}${order.address1}${addr2}`
}

function goChat(order: OrderResponse) {
  router.push({ path: '/chat', query: { orderNo: order.orderNo } })
}

onMounted(load)
</script>

<template>
  <section>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-else-if="loading">불러오는 중...</p>
    <p v-else-if="orders.length === 0">주문 내역이 없습니다.</p>

    <table v-else class="order-table">
      <thead>
        <tr>
          <th>주문번호</th>
          <th>주문상품</th>
          <th class="order-price">가격</th>
          <th>상태</th>
          <th>배송지</th>
          <th>주문일시</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="order in orders" :key="order.id">
          <td>{{ order.orderNo }}</td>
          <td>{{ itemsSummary(order) }}</td>
          <td class="order-price">{{ orderTotal(order) }}</td>
          <td><span :class="['status', order.status]">{{ statusLabel(order.status) }}</span></td>
          <td class="shipping-cell">
            <div class="recipient">{{ order.recipientName }} ({{ order.recipientPhone }})</div>
            <div class="address">{{ shippingAddress(order) }}</div>
          </td>
          <td>{{ new Date(order.orderedAt).toLocaleString() }}</td>
          <td><button type="button" @click="goChat(order)">CS채팅</button></td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.error {
  color: #a33;
}
.order-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.order-table th,
.order-table td {
  border-bottom: 1px solid #eee;
  padding: 10px;
  text-align: left;
}
.order-price {
  white-space: nowrap;
}
.shipping-cell {
  font-size: 13px;
  max-width: 220px;
}
.shipping-cell .recipient {
  color: #333;
  font-weight: 600;
  margin-bottom: 2px;
}
.shipping-cell .address {
  color: #777;
}
.status {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  background: #eee;
  white-space: nowrap;
}
.status.CANCELLED,
.status.RETURNED {
  background: #f2f2f2;
  color: #888;
}
.status.RETURN_REQUESTED {
  background: #fde2c8;
  color: #a35b00;
}
.status.DELIVERED {
  background: #dff0d8;
  color: #3c763d;
}
.order-table button {
  padding: 6px 12px;
  border: 1px solid #0056b3;
  border-radius: 6px;
  background: #fff;
  color: #0056b3;
  font-size: 13px;
  cursor: pointer;
}
</style>
