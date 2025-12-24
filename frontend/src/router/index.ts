import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'dashboard',
            component: () => import('../views/Dashboard.vue')
        },
        {
            path: '/ponds',
            name: 'ponds',
            component: () => import('../views/Ponds.vue')
        },
        {
            path: '/batches',
            name: 'batches',
            component: () => import('../views/Batches.vue')
        },
        {
            path: '/operations',
            name: 'operations',
            component: () => import('../views/Operations.vue')
        },
        {
            path: '/harvest',
            name: 'harvest',
            component: () => import('../views/Harvest.vue')
        },
        {
            path: '/payroll',
            name: 'payroll',
            component: () => import('../views/Payroll.vue')
        },
        {
            path: '/finance',
            name: 'finance',
            component: () => import('../views/Finance.vue')
        },
        {
            path: '/settings',
            name: 'settings',
            component: () => import('../views/Settings.vue')
        },
        {
            path: '/suppliers',
            name: 'suppliers',
            component: () => import('../views/Suppliers.vue')
        },
        {
            path: '/feed-stock',
            name: 'feed-stock',
            component: () => import('../views/FeedStock.vue')
        },
        {
            path: '/feed-orders',
            name: 'feed-orders',
            component: () => import('../views/FeedOrders.vue')
        }
    ]
})

export default router
