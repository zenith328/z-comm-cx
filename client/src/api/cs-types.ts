export type Gender = 'MALE' | 'FEMALE'

export interface MemberResponse {
  name: string
  phone: string
  gender: Gender | null
  birthYear: number | null
  // 서버가 birthYear로부터 계산해서 내려주는 파생값. 세그먼트 매칭 등 기존 로직은 이 값을 그대로 쓴다.
  age: number | null
  // "내 체형 맞춤 핏 요약"에서만 쓰인다. 세그먼트/개인화 매칭에는 관여하지 않는다.
  heightCm: number | null
  weightKg: number | null
  createdAt: string
  updatedAt: string
}

export interface MemberLoginResponse {
  member: MemberResponse
  firstLogin: boolean
}

export interface MemberProfileUpdateRequest {
  name: string
  phone: string
  gender: Gender | null
  birthYear: number | null
  heightCm: number | null
  weightKg: number | null
}

export type CustomerSegment =
  | 'MALE_10_20S'
  | 'MALE_30_40S'
  | 'MALE_50S_PLUS'
  | 'FEMALE_10_20S'
  | 'FEMALE_30_40S'
  | 'FEMALE_50S_PLUS'

export interface SegmentKeywordResponse {
  segment: CustomerSegment
  segmentLabel: string
  gender: Gender
  keywords: string | null
  updatedAt: string | null
}

export interface SegmentKeywordRequest {
  keywords: string
}

export interface SegmentKeywordSuggestionResponse {
  keywords: string[]
  reviewCount: number
}

export interface DescriptionExtractResponse {
  text: string
}

export interface ProductDescriptionResponse {
  text: string | null
  personalized: boolean
}

export type DescriptionVariantStatus = 'NOT_GENERATED' | 'DRAFT' | 'APPROVED'

export interface ProductDescriptionVariantResponse {
  segment: CustomerSegment
  segmentLabel: string
  content: string | null
  status: DescriptionVariantStatus
  generatedAt: string | null
  approvedAt: string | null
  fitScore: number | null
  fitScoreReason: string | null
}

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
