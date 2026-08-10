import { onMounted, ref } from 'vue'
import {
  fetchBrands,
  fetchProducts,
  markOutOfStock as apiMarkOutOfStock,
  registerProduct as apiRegisterProduct,
  restockProduct as apiRestockProduct,
} from '../api/products'
import type { Product } from '../types/product'

const DEFAULT_PAGE_SIZE = 10

export function useProducts(options: { pageSize?: number } = {}) {
  const pageSize = options.pageSize ?? DEFAULT_PAGE_SIZE
  const products = ref<Product[]>([])
  const loading = ref(false)
  const errorMessage = ref('')
  const page = ref(0)
  const totalPages = ref(1)
  const totalElements = ref(0)
  const productCodeFilter = ref('')
  const brands = ref<string[]>([])
  const brandFilter = ref<string | null>(null)

  async function refresh() {
    loading.value = true
    try {
      const result = await fetchProducts({
        page: page.value,
        size: pageSize,
        productCode: productCodeFilter.value.trim() || undefined,
        brand: brandFilter.value ?? undefined,
      })
      products.value = result.content
      totalPages.value = result.totalPages
      totalElements.value = result.totalElements
      errorMessage.value = ''
    } catch (error) {
      console.error(error)
      errorMessage.value = '상품 목록을 불러오지 못했습니다.'
    } finally {
      loading.value = false
    }
  }

  async function loadBrands() {
    try {
      brands.value = await fetchBrands()
    } catch (error) {
      console.error(error)
    }
  }

  function setBrandFilter(value: string | null) {
    brandFilter.value = value
    page.value = 0
    refresh()
  }

  async function registerProduct(url: string): Promise<Product> {
    const product = await apiRegisterProduct({ url })
    page.value = 0
    await Promise.all([refresh(), loadBrands()])
    return product
  }

  function setProductCodeFilter(value: string) {
    productCodeFilter.value = value
    page.value = 0
    refresh()
  }

  function goToPage(target: number) {
    if (target < 0 || target >= totalPages.value) return
    page.value = target
    refresh()
  }

  function applyInventory(productId: number, stockQuantity: number, reservedQuantity: number) {
    const target = products.value.find((product) => product.id === productId)
    if (!target) return
    target.stockQuantity = stockQuantity
    target.reservedQuantity = reservedQuantity
    target.availableQuantity = stockQuantity - reservedQuantity
  }

  async function restock(productId: number, quantity: number) {
    const updated = await apiRestockProduct(productId, quantity)
    applyInventory(updated.productId, updated.quantity, updated.reservedQuantity)
  }

  async function markOutOfStock(productId: number) {
    const updated = await apiMarkOutOfStock(productId)
    applyInventory(updated.productId, updated.quantity, updated.reservedQuantity)
  }

  onMounted(() => {
    refresh()
    loadBrands()
  })

  return {
    products,
    loading,
    errorMessage,
    page,
    totalPages,
    totalElements,
    productCodeFilter,
    brands,
    brandFilter,
    refresh,
    registerProduct,
    setProductCodeFilter,
    setBrandFilter,
    goToPage,
    restock,
    markOutOfStock,
  }
}
