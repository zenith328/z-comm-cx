<script setup lang="ts">
import { useRouter } from 'vue-router'
import ProductGrid from '../components/ProductGrid.vue'
import { useProducts } from '../composables/useProducts'
import type { Product } from '../types/product'

const router = useRouter()
const { products, loading, errorMessage, brands, brandFilter, setBrandFilter } = useProducts({
  pageSize: 200,
})

function openDetail(product: Product) {
  router.push(`/products/${product.id}`)
}
</script>

<template>
  <ProductGrid
    :products="products"
    :loading="loading"
    :error-message="errorMessage"
    :brands="brands"
    :selected-brand="brandFilter"
    @select="openDetail"
    @brand-select="setBrandFilter"
  />
</template>
