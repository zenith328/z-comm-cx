import { createRouter, createWebHistory } from 'vue-router'
import { session } from '../stores/session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/admin/products' },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    {
      path: '/products',
      name: 'products',
      component: () => import('../views/ProductListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/products/:id',
      name: 'product-detail',
      component: () => import('../views/ProductDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/products/:id/review',
      name: 'review-write',
      component: () => import('../views/ReviewWriteView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/orders/new',
      name: 'order-new',
      component: () => import('../views/OrderCheckoutView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('../views/OrderListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('../views/CustomerChatView.vue'),
      meta: { requiresAuth: true },
    },
    { path: '/admin', redirect: '/admin/products' },
    { path: '/admin/products', name: 'admin-products', component: () => import('../views/AdminProductView.vue') },
    { path: '/admin/reviews', name: 'admin-reviews', component: () => import('../views/AdminReviewView.vue') },
    { path: '/admin/orders', name: 'admin-orders', component: () => import('../views/AdminOrderView.vue') },
    { path: '/admin/tickets', name: 'admin-tickets', component: () => import('../views/AdminCsTicketView.vue') },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !session.current) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
