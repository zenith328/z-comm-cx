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
