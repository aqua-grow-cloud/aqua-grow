import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import FeedOrdersPage from './FeedOrdersPage.vue'
import { useTenantStore } from '@/stores/useTenantStore'

vi.mock('@/api/manual/hooks', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/manual/hooks')>()
  return {
    ...actual,
    useGetAllOrders: () => ({ data: { value: [] }, isLoading: { value: false } }),
    useGetAllSuppliers: () => ({ data: { value: [] } }),
    useCreateOrder: () => ({ mutateAsync: vi.fn() }),
    useConfirmOrder: () => ({ mutateAsync: vi.fn() }),
    useReceiveOrder: () => ({ mutateAsync: vi.fn() }),
    useCancelOrder: () => ({ mutateAsync: vi.fn() }),
    useUpdateOrderPaymentStatus: () => ({ mutateAsync: vi.fn() }),
    useGetOrderMessage: () => ({ data: { value: null } }),
    useGetAttachedSupplier: () => ({ data: { value: null } }),
    useGetAllFeedStock: () => ({ data: { value: [] }, isLoading: { value: false } }),
  }
})

describe('FeedOrdersPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('shows "Place Order" button for root tenant', () => {
    const store = useTenantStore()
    vi.spyOn(store, 'isSubTenant', 'get').mockReturnValue(false)

    const wrapper = mount(FeedOrdersPage, {
      global: {
        stubs: {
          PageHeader: {
            template: '<div><slot /><button v-if="actionLabel">{{ actionLabel }}</button></div>',
            props: ['title', 'actionLabel'],
          },
          VContainer: { template: '<div><slot /></div>' },
          VCard: { template: '<div><slot /></div>' },
          VDataTable: { template: '<div><slot /></div>' },
          VDialog: { template: '<div><slot /></div>' },
          VBtn: { template: '<button><slot /></button>' },
          VIcon: { template: '<span><slot /></span>' },
          VTooltip: { template: '<div><slot name="activator" :props="{}" /></div>' },
          VChip: { template: '<span><slot /></span>' },
          VForm: { template: '<form><slot /></form>' },
          VTextField: { template: '<input />' },
          VSelect: { template: '<select />' },
          VCombobox: { template: '<input />' },
          VSpacer: { template: '<span />' },
          VCardTitle: { template: '<div><slot /></div>' },
          VCardText: { template: '<div><slot /></div>' },
          VCardActions: { template: '<div><slot /></div>' },
          OrderMessageDialog: { template: '<div />' },
        },
      },
    })

    expect(wrapper.text()).toContain('Place Order')
  })

  it('hides "Place Order" button for sub-tenant', () => {
    const store = useTenantStore()
    vi.spyOn(store, 'isSubTenant', 'get').mockReturnValue(true)

    const wrapper = mount(FeedOrdersPage, {
      global: {
        stubs: {
          PageHeader: {
            template: '<div><slot /><button v-if="actionLabel">{{ actionLabel }}</button></div>',
            props: ['title', 'actionLabel'],
          },
          VContainer: { template: '<div><slot /></div>' },
          VCard: { template: '<div><slot /></div>' },
          VDataTable: { template: '<div><slot /></div>' },
          VDialog: { template: '<div><slot /></div>' },
          VBtn: { template: '<button><slot /></button>' },
          VIcon: { template: '<span><slot /></span>' },
          VTooltip: { template: '<div><slot name="activator" :props="{}" /></div>' },
          VChip: { template: '<span><slot /></span>' },
          VForm: { template: '<form><slot /></form>' },
          VTextField: { template: '<input />' },
          VSelect: { template: '<select />' },
          VCombobox: { template: '<input />' },
          VSpacer: { template: '<span />' },
          VCardTitle: { template: '<div><slot /></div>' },
          VCardText: { template: '<div><slot /></div>' },
          VCardActions: { template: '<div><slot /></div>' },
          OrderMessageDialog: { template: '<div />' },
        },
      },
    })

    expect(wrapper.text()).not.toContain('Place Order')
  })
})
