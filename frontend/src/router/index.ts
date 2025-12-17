import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'home',
            component: () => import('../views/Home.vue')
        },
        {
            path: '/ponds',
            name: 'ponds',
            component: () => import('../views/Ponds.vue')
        }
    ]
})

export default router
