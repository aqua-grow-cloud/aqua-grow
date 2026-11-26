import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import FeedStockPage from './FeedStockPage.vue'
import { useTenantStore } from '@/stores/useTenantStore'

vi.mock('@/api/manual/hooks', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/manual/hooks')>()
  return {
    ...actual,
    useGetAllFeedStock: () => ({ data: { value: [] }, isLoading: { value: false } }),
    useGetAllSuppliers: () => ({ data: { value: [] } }),
    useCreateFeedStock: () => ({ mutateAsync: vi.fn() }),
    useUpdateFeedStock: () => ({ mutateAsync: vi.fn() }),
    useDeleteFeedStock: () => ({ mutateAsync: vi.fn() }),
    useGetLowStock: () => ({ data: { value: [] } }),
    useCreateOrder: () => ({ mutateAsync: vi.fn() }),
    useGetAllOrders: () => ({ data: { value: [] } }),
    useGetOrderMessage: () => ({ data: { value: null } }),
    useConfirmOrder: () => ({ mutateAsync: vi.fn() }),
    useReceiveOrder: () => ({ mutateAsync: vi.fn() }),
    useCancelOrder: () => ({ mutateAsync: vi.fn() }),
    useUpdateOrderPaymentStatus: () => ({ mutateAsync: vi.fn() }),
    useGetAttachedSupplier: () => ({ data: { value: null } }),
  }
})

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')
  return { ...actual, useRouter: () => ({ push: vi.fn() }) }
})

describe('FeedStockPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('shows "Add Feed Stock" button for root tenant', async () => {
    const store = useTenantStore()
    vi.spyOn(store, 'isSubTenant', 'get').mockReturnValue(false)

    const wrapper = mount(FeedStockPage, {
      global: {
        stubs: {
          PageHeader: {
            template: '<div><slot /><button v-if="actionLabel">{{ actionLabel }}</button></div>',
            props: ['title', 'actionLabel'],
          },
          VContainer: { template: '<div><slot /></div>' },
          VRow: { template: '<div><slot /></div>' },
          VCol: { template: '<div><slot /></div>' },
          VCard: { template: '<div><slot /></div>' },
          VDataTable: { template: '<div><slot /></div>' },
          VAlert: { template: '<div :data-testid="$attrs[\'data-testid\']"><slot /></div>', inheritAttrs: false },
          VDialog: { template: '<div><slot /></div>' },
          VDivider: { template: '<div />' },
          VBtn: { template: '<button><slot /></button>' },
          VIcon: { template: '<span><slot /></span>' },
          VTooltip: { template: '<div><slot name="activator" :props="{}" /></div>' },
          VBadge: { template: '<span />' },
          VChip: { template: '<span><slot /></span>' },
          VForm: { template: '<form><slot /></form>' },
          VTextField: { template: '<input />' },
          VSelect: { template: '<select />' },
          VSpacer: { template: '<span />' },
          VCardTitle: { template: '<div><slot /></div>' },
          VCardText: { template: '<div><slot /></div>' },
          VCardActions: { template: '<div><slot /></div>' },
        },
      },
    })

    expect(wrapper.text()).toContain('Add Feed Stock')
    expect(wrapper.find('[data-testid="sub-tenant-banner"]').exists()).toBe(false)
  })

  it('hides write actions and shows banner for sub-tenant', async () => {
    const store = useTenantStore()
    vi.spyOn(store, 'isSubTenant', 'get').mockReturnValue(true)

    const wrapper = mount(FeedStockPage, {
      global: {
        stubs: {
          PageHeader: {
            template: '<div><slot /><button v-if="actionLabel">{{ actionLabel }}</button></div>',
            props: ['title', 'actionLabel'],
          },
          VContainer: { template: '<div><slot /></div>' },
          VRow: { template: '<div><slot /></div>' },
          VCol: { template: '<div><slot /></div>' },
          VCard: { template: '<div><slot /></div>' },
          VDataTable: { template: '<div><slot /></div>' },
          VAlert: { template: '<div :data-testid="$attrs[\'data-testid\']"><slot /></div>', inheritAttrs: false },
          VDialog: { template: '<div><slot /></div>' },
          VDivider: { template: '<div />' },
          VBtn: { template: '<button><slot /></button>' },
          VIcon: { template: '<span><slot /></span>' },
          VTooltip: { template: '<div><slot name="activator" :props="{}" /></div>' },
          VBadge: { template: '<span />' },
          VChip: { template: '<span><slot /></span>' },
          VForm: { template: '<form><slot /></form>' },
          VTextField: { template: '<input />' },
          VSelect: { template: '<select />' },
          VSpacer: { template: '<span />' },
          VCardTitle: { template: '<div><slot /></div>' },
          VCardText: { template: '<div><slot /></div>' },
          VCardActions: { template: '<div><slot /></div>' },
        },
      },
    })

    expect(wrapper.text()).not.toContain('Add Feed Stock')
    expect(wrapper.find('[data-testid="sub-tenant-banner"]').exists()).toBe(true)
  })
})
