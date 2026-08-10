import axios from 'axios'
import type { Product, ProductCreateRequest } from '../types/product'
import type { PageResponse } from '../types/page'
import type { InventoryResponse } from './cs-types'

const client = axios.create({ baseURL: '/api' })

export interface FetchProductsParams {
  page: number
  size: number
  productCode?: string
  brand?: string
}

export function fetchProducts(params: FetchProductsParams): Promise<PageResponse<Product>> {
  return client.get<PageResponse<Product>>('/products', { params }).then((res) => res.data)
}

export function fetchBrands(): Promise<string[]> {
  return client.get<string[]>('/products/brands').then((res) => res.data)
}

export function fetchProduct(id: number): Promise<Product> {
  return client.get<Product>(`/products/${id}`).then((res) => res.data)
}

export function registerProduct(request: ProductCreateRequest): Promise<Product> {
  return client.post<Product>('/products', request).then((res) => res.data)
}

export function restockProduct(id: number, quantity: number): Promise<InventoryResponse> {
  return client.post<InventoryResponse>(`/products/${id}/inventory/restock`, { quantity }).then((res) => res.data)
}

export function markOutOfStock(id: number): Promise<InventoryResponse> {
  return client.post<InventoryResponse>(`/products/${id}/inventory/out-of-stock`).then((res) => res.data)
}
