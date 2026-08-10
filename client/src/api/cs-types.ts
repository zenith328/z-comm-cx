export type TicketCategory = 'CANCEL' | 'ADDRESS_CHANGE' | 'RETURN' | 'INQUIRY' | 'OTHER'
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'ESCALATED' | 'CLOSED'
export type TicketChannel = 'AI' | 'HUMAN'

export interface TicketResponse {
  id: number
  ticketNo: string
  orderId: number | null
  orderNo: string | null
  customerName: string | null
  customerPhone: string | null
  category: TicketCategory
  status: TicketStatus
  channel: TicketChannel
  summary: string
  resolution: string | null
  chatTranscript: string | null
  createdAt: string
  resolvedAt: string | null
}

export interface ChatResponse {
  sessionId: string
  reply: string
}

export interface GuardrailResponse {
  returnWindowDays: number
}

export interface InventoryResponse {
  productId: number
  quantity: number
  reservedQuantity: number
  availableQuantity: number
  updatedAt: string
}

export type OrderStatus = 'PAID' | 'PREPARING' | 'SHIPPING' | 'DELIVERED' | 'CANCELLED' | 'RETURN_REQUESTED' | 'RETURNED'

export interface OrderItemResponse {
  productId: number
  productName: string
  unitPrice: number
  quantity: number
}

export interface OrderResponse {
  id: number
  orderNo: string
  customerName: string
  customerPhone: string
  status: OrderStatus
  recipientName: string
  recipientPhone: string
  zipcode: string | null
  address1: string
  address2: string | null
  orderedAt: string
  shippedAt: string | null
  statusReason: string | null
  items: OrderItemResponse[]
}

export interface OrderCreateRequest {
  customerName: string
  customerPhone: string
  recipientName: string
  recipientPhone: string
  zipcode: string
  address1: string
  address2: string
  items: { productId: number; quantity: number }[]
}
