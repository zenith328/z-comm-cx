import { http } from './http'
import type { TicketResponse } from './cs-types'

export function listTickets() {
  return http.get<TicketResponse[]>('/tickets').then((res) => res.data)
}

export function escalateTicket(id: number) {
  return http.post<TicketResponse>(`/tickets/${id}/escalate`).then((res) => res.data)
}

export function startTicketProgress(id: number) {
  return http.post<TicketResponse>(`/tickets/${id}/start-progress`).then((res) => res.data)
}

export function resolveTicket(id: number, resolution: string) {
  return http.post<TicketResponse>(`/tickets/${id}/resolve`, { resolution }).then((res) => res.data)
}
