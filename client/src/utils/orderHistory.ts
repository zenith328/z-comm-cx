const CUSTOMER_KEY = 'cs-auto-resolver:customerHistory'
const SHIPPING_KEY = 'cs-auto-resolver:shippingHistory'
const MAX_HISTORY = 5

export interface CustomerHistoryEntry {
  customerName: string
  customerPhone: string
}

export interface ShippingHistoryEntry {
  recipientName: string
  recipientPhone: string
  zipcode: string
  address1: string
  address2: string
}

function readList<T>(key: string): T[] {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T[]) : []
  } catch {
    return []
  }
}

function writeList<T>(key: string, list: T[]) {
  localStorage.setItem(key, JSON.stringify(list))
}

export function getCustomerHistory(): CustomerHistoryEntry[] {
  return readList<CustomerHistoryEntry>(CUSTOMER_KEY)
}

export function saveCustomerHistory(entry: CustomerHistoryEntry) {
  const list = getCustomerHistory().filter(
    (e) => !(e.customerName === entry.customerName && e.customerPhone === entry.customerPhone),
  )
  list.unshift(entry)
  writeList(CUSTOMER_KEY, list.slice(0, MAX_HISTORY))
}

export function getShippingHistory(): ShippingHistoryEntry[] {
  return readList<ShippingHistoryEntry>(SHIPPING_KEY)
}

export function saveShippingHistory(entry: ShippingHistoryEntry) {
  const list = getShippingHistory().filter(
    (e) =>
      !(
        e.recipientName === entry.recipientName &&
        e.recipientPhone === entry.recipientPhone &&
        e.zipcode === entry.zipcode &&
        e.address1 === entry.address1 &&
        e.address2 === entry.address2
      ),
  )
  list.unshift(entry)
  writeList(SHIPPING_KEY, list.slice(0, MAX_HISTORY))
}
