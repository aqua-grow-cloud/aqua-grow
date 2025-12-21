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
            path: '/settings',
            name: 'settings',
            component: () => import('../views/Settings.vue')
        }
    ]
})

export default router
