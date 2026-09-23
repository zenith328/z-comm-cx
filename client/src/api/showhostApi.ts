import { http } from './http'
import type {
  ShowhostDiscountSetting,
  ShowhostOffer,
  ShowhostQnaResponse,
  ShowhostResponse,
} from '../types/showhost'

export function fetchShowhost(productCode: string): Promise<ShowhostResponse> {
  return http.get<ShowhostResponse>(`/products/${productCode}/showhost`).then((res) => res.data)
}

export function regenerateShowhost(productCode: string): Promise<ShowhostResponse> {
  return http.post<ShowhostResponse>(`/products/${productCode}/showhost/regenerate`).then((res) => res.data)
}

export function askShowhost(productCode: string, question: string): Promise<ShowhostQnaResponse> {
  return http
    .post<ShowhostQnaResponse>(`/products/${productCode}/showhost/qna`, { question })
    .then((res) => res.data)
}

export function fetchShowhostDiscount(productCode: string): Promise<ShowhostDiscountSetting> {
  return http.get<ShowhostDiscountSetting>(`/products/${productCode}/showhost/discount`).then((res) => res.data)
}

export function updateShowhostDiscount(
  productCode: string,
  setting: ShowhostDiscountSetting,
): Promise<ShowhostDiscountSetting> {
  return http
    .put<ShowhostDiscountSetting>(`/products/${productCode}/showhost/discount`, setting)
    .then((res) => res.data)
}

// 서버는 특가가 없으면 204(본문 없음)를 준다 — 그때는 null.
function offerOrNull(res: { status: number; data: ShowhostOffer | '' }): ShowhostOffer | null {
  return res.status === 204 || !res.data ? null : res.data
}

/** 방송에 들어온 고객에게 방송 특가를 발급한다(오늘 이미 받았으면 그대로 반환). */
export function issueShowhostOffer(
  productCode: string,
  customerName: string,
  customerPhone: string,
): Promise<ShowhostOffer | null> {
  return http
    .post<ShowhostOffer | ''>(`/products/${productCode}/showhost/offer`, { customerName, customerPhone })
    .then(offerOrNull)
}

/** 주문 화면용 — 오늘 받은 방송 특가를 조회만 한다(발급하지 않음). */
export function fetchShowhostOffer(
  productCode: string,
  customerName: string,
  customerPhone: string,
): Promise<ShowhostOffer | null> {
  return http
    .get<ShowhostOffer | ''>(`/products/${productCode}/showhost/offer`, { params: { customerName, customerPhone } })
    .then(offerOrNull)
}

/** 서버(ShowhostDiscount.discountedPrice)와 같은 계산 — 화면 표시용일 뿐 실제 결제금액은 서버가 정한다. */
export function discountedPrice(price: number, discountRate: number): number {
  return Math.floor((price * (100 - discountRate)) / 100)
}
