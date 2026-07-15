import {createRouter, createWebHistory} from 'vue-router'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'dashboard',
            component: () => import('../features/operations/DashboardPage.vue')
        },
        {
            path: '/ponds',
            name: 'ponds',
            component: () => import('../features/ponds/PondsPage.vue')
        },
        {
            path: '/batches',
            name: 'batches',
            component: () => import('../features/batches/BatchesPage.vue')
        },
        {
            path: '/batches/:id',
            name: 'batch-detail',
            component: () => import('../features/batches/BatchDetailPage.vue')
        },
        {
            path: '/operations',
            name: 'operations',
            component: () => import('../features/operations/OperationsPage.vue')
        },
        {
            path: '/operations/sample-logs',
            name: 'sample-logs-history',
            component: () => import('../features/operations/SampleLogsPage.vue')
        },
        {
            path: '/operations/monitoring-logs',
            name: 'monitoring-logs-history',
            component: () => import('../features/operations/MonitoringLogsPage.vue')
        },
        {
            path: '/harvest',
            name: 'harvest',
            component: () => import('../features/harvesting/HarvestPage.vue')
        },
        {
            path: '/harvest/simulation',
            name: 'harvest-simulation',
            component: () => import('../features/harvesting/HarvestSimulationPage.vue')
        },
        {
            path: '/payroll',
            name: 'payroll',
            component: () => import('../features/payroll/PayrollPage.vue')
        },
        {
            path: '/finance',
            name: 'finance',
            component: () => import('../features/finance/FinancePage.vue')
        },
        {
            path: '/settings',
            name: 'settings',
            component: () => import('../features/settings/SettingsPage.vue')
        },
        {
            path: '/suppliers',
            name: 'suppliers',
            component: () => import('../features/inventory/SuppliersPage.vue')
        },
        {
            path: '/feed-stock',
            name: 'feed-stock',
            component: () => import('../features/inventory/FeedStockPage.vue')
        },
        {
            path: '/feed-orders',
            name: 'feed-orders',
            component: () => import('../features/inventory/FeedOrdersPage.vue')
        }
    ]
})

export default router
