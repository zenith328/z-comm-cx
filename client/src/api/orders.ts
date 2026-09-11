import { http } from './http'
import type { OrderCreateRequest, OrderResponse } from './cs-types'

export function listOrders() {
  return http.get<OrderResponse[]>('/orders').then((res) => res.data)
}

export function getOrder(id: number) {
  return http.get<OrderResponse>(`/orders/${id}`).then((res) => res.data)
}

export function createOrder(request: OrderCreateRequest) {
  return http.post<OrderResponse>('/orders', request).then((res) => res.data)
}

export function shipOrder(id: number) {
  return http.post<OrderResponse>(`/orders/${id}/ship`).then((res) => res.data)
}

export function deliverOrder(id: number) {
  return http.post<OrderResponse>(`/orders/${id}/deliver`).then((res) => res.data)
}

/** 반품접수 상태의 주문을 최종 확정한다(반품완료 처리 + CX-Pay 환불). */
export function completeReturn(id: number) {
  return http.post<OrderResponse>(`/orders/${id}/complete-return`).then((res) => res.data)
}
