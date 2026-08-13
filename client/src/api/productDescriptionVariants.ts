import { http } from './http'
import type {
  CustomerSegment,
  DescriptionExtractResponse,
  Gender,
  ProductDescriptionResponse,
  ProductDescriptionVariantResponse,
} from './cs-types'

export function fetchProductDescriptionVariants(productId: number) {
  return http
    .get<ProductDescriptionVariantResponse[]>(`/products/${productId}/description-variants`)
    .then((res) => res.data)
}

export function generateProductDescriptionVariant(productId: number, segment: CustomerSegment) {
  return http
    .post<ProductDescriptionVariantResponse>(`/products/${productId}/description-variants/${segment}/generate`)
    .then((res) => res.data)
}

export function generateAllProductDescriptionVariants(productId: number) {
  return http
    .post<ProductDescriptionVariantResponse[]>(`/products/${productId}/description-variants/generate-all`)
    .then((res) => res.data)
}

export function approveProductDescriptionVariant(productId: number, segment: CustomerSegment) {
  return http
    .post<ProductDescriptionVariantResponse>(`/products/${productId}/description-variants/${segment}/approve`)
    .then((res) => res.data)
}

export function editProductDescriptionVariant(productId: number, segment: CustomerSegment, content: string) {
  return http
    .put<ProductDescriptionVariantResponse>(`/products/${productId}/description-variants/${segment}`, { content })
    .then((res) => res.data)
}

export function deleteProductDescriptionVariant(productId: number, segment: CustomerSegment) {
  return http
    .delete<ProductDescriptionVariantResponse>(`/products/${productId}/description-variants/${segment}`)
    .then((res) => res.data)
}

export function extractDescriptionFromUrl(productId: number, url: string) {
  return http
    .post<DescriptionExtractResponse>(`/products/${productId}/description/extract-from-url`, { url })
    .then((res) => res.data.text)
}

export function fetchResolvedDescription(productId: number, gender: Gender | null, age: number | null) {
  const params: Record<string, string | number> = {}
  if (gender) params.gender = gender
  if (age != null) params.age = age
  return http
    .get<ProductDescriptionResponse>(`/products/${productId}/description`, { params })
    .then((res) => res.data)
}
