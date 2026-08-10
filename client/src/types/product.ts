export interface Product {
  id: number
  sourceUrl: string
  productCode: string
  name: string
  brand: string | null
  price: number | null
  imageUrls: string[]
  reviewCount: number
  stockQuantity: number | null
  reservedQuantity: number | null
  availableQuantity: number | null
  createdAt: string
}

export interface ProductCreateRequest {
  url: string
}
