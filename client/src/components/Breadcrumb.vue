<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { breadcrumbState } from '../stores/breadcrumb'

interface Crumb {
  label: string
  to?: string
}

const route = useRoute()

// 최상위 탭(상품목록/주문목록/CS채팅)은 sub-tabs에 이미 표시되어 있으므로,
// 그 아래로 한 단계 이상 들어간 화면에서만 브레드크럼을 보여준다.
const crumbs = computed<Crumb[]>(() => {
  switch (route.name) {
    case 'product-detail':
      return [
        { label: '상품목록', to: '/products' },
        { label: breadcrumbState.productName ?? '상품상세' },
      ]
    case 'review-write':
      return [
        { label: '상품목록', to: '/products' },
        { label: breadcrumbState.productName ?? '상품상세', to: `/products/${route.params.id}` },
        { label: '리뷰 작성' },
      ]
    case 'order-new':
      return [
        { label: '주문목록', to: '/orders' },
        { label: '주문서 작성' },
      ]
    default:
      return []
  }
})
</script>

<template>
  <nav v-if="crumbs.length" class="breadcrumb">
    <template v-for="(crumb, index) in crumbs" :key="index">
      <RouterLink v-if="crumb.to" :to="crumb.to">{{ crumb.label }}</RouterLink>
      <span v-else class="current">{{ crumb.label }}</span>
      <span v-if="index < crumbs.length - 1" class="sep">›</span>
    </template>
  </nav>
</template>

<style scoped>
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 16px;
  font-size: 13px;
  color: #888;
}
.breadcrumb a {
  color: #0056b3;
  text-decoration: none;
}
.breadcrumb a:hover {
  text-decoration: underline;
}
.breadcrumb .current {
  color: #333;
  font-weight: 600;
}
.breadcrumb .sep {
  color: #ccc;
}
</style>
