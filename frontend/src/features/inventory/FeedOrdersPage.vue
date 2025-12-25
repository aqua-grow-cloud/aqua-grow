<template>
  <v-container fluid>
    <PageHeader 
        title="Feed Orders" 
        action-label="New Order" 
        @action="openCreateDialog" 
    />

    <v-card variant="outlined" class="rounded-lg mt-4">
        <v-data-table
            :headers="headers"
            :items="orders || []"
            :loading="isLoading"
            :sort-by="[{ key: 'orderDate', order: 'desc' }]"
        >
             <template v-slot:item.orderDate="{ item }">
                {{ formatDate(item.orderDate) }}
            </template>
            
            <template v-slot:item.supplierId="{ item }">
                {{ getSupplierName(item.supplierId) }}
            </template>

            <template v-slot:item.status="{ item }">
                <v-chip :color="getStatusColor(item.status)" size="small" label>
                    {{ item.status }}
                </v-chip>
            </template>

            <template v-slot:item.actions="{ item }">
                <template v-if="item.status === 'PENDING'">
                    <v-tooltip text="Get WhatsApp Message">
                        <template v-slot:activator="{ props }">
                            <v-btn v-bind="props" icon size="small" variant="text" color="success" @click="showMessage(item)">
                                <v-icon>mdi-whatsapp</v-icon>
                            </v-btn>
                        </template>
                    </v-tooltip>
                    <v-tooltip text="Confirm Order">
                        <template v-slot:activator="{ props }">
                            <v-btn v-bind="props" icon size="small" variant="text" color="primary" @click="confirmOrder(item)">
                                <v-icon>mdi-check-circle-outline</v-icon>
                            </v-btn>
                        </template>
                    </v-tooltip>
                </template>

                <template v-if="item.status === 'CONFIRMED'">
                     <v-tooltip text="Mark Received (Updates Stock)">
                        <template v-slot:activator="{ props }">
                            <v-btn v-bind="props" icon size="small" variant="text" color="indigo" @click="receiveOrder(item)">
                                <v-icon>mdi-package-variant-closed</v-icon>
                            </v-btn>
                        </template>
                    </v-tooltip>
                </template>

                <v-btn v-if="item.status !== 'RECEIVED' && item.status !== 'CANCELLED'" 
                    icon size="small" variant="text" color="error" @click="cancelOrder(item)">
                    <v-icon>mdi-close</v-icon>
                </v-btn>
            </template>
        </v-data-table>
    </v-card>

     <!-- Create Order Dialog -->
    <v-dialog v-model="createDialog" max-width="500px">
        <v-card>
            <v-card-title>Create New Order</v-card-title>
            <v-card-text>
                <v-form @submit.prevent="submitOrder">
                     <v-combobox
                        v-model="orderForm.feedType"
                        label="Feed Type"
                        variant="outlined"
                        density="compact"
                        :items="feedTypes"
                    ></v-combobox>
                     <v-text-field
                        v-model.number="orderForm.quantity"
                        label="Quantity (kg)"
                        type="number"
                        variant="outlined"
                        density="compact"
                    ></v-text-field>
                     <v-select
                        v-model="orderForm.supplierId"
                        :items="suppliers || []"
                        item-title="name"
                        item-value="id"
                        label="Supplier"
                        variant="outlined"
                        density="compact"
                    ></v-select>
                    <v-btn block color="primary" type="submit" class="mt-4">Create Order</v-btn>
                </v-form>
            </v-card-text>
        </v-card>
    </v-dialog>

    <OrderMessageDialog 
        v-model="messageDialog" 
        :order-id="selectedOrderId"
        location="Kaikaram, lankamalapalli road"
        contact-person="Farm Manager"
    />
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAquaStore } from '../../stores/useAquaStore';
import PageHeader from '../../components/PageHeader.vue';
import OrderMessageDialog from './components/OrderMessageDialog.vue';
import { feedOrderService } from './api/feedOrderService';
import { supplierService } from '../inventory/api/supplierService';
import { feedStockService } from '../inventory/api/feedStockService';
import type { FeedOrder, FeedStock, Supplier } from '../../api/manual/types';

const headers = [
  { title: 'Date', key: 'orderDate' },
  { title: 'Feed Type', key: 'feedType' },
  { title: 'Quantity (kg)', key: 'quantity' },
  { title: 'Supplier', key: 'supplierId' },
  { title: 'Status', key: 'status' },
  { title: 'Actions', key: 'actions', sortable: false },
];

const { data: orders, isLoading } = feedOrderService.useGetAll();
const { data: suppliers } = supplierService.useGetAll();
const { data: attachedSupplier } = supplierService.useGetAttached();
const { data: feedStocks } = feedStockService.useGetAll();

const createMutation = feedOrderService.useCreate();
const confirmMutation = feedOrderService.useConfirm();
const receiveMutation = feedOrderService.useReceive();
const cancelMutation = feedOrderService.useCancel();

const createDialog = ref(false);
const messageDialog = ref(false);
const selectedOrderId = ref<string>('');

const orderForm = ref({
    feedType: '',
    quantity: 0,
    supplierId: ''
});

// Extract unique feed types from stock
const feedTypes = computed(() => {
    return feedStocks.value ? [...new Set(feedStocks.value.map((s: FeedStock) => s.feedType))] : [];
});

function getSupplierName(id: string) {
    const s = suppliers.value?.find((sup: Supplier) => sup.id === id);
    return s ? s.name : id;
}

function getStatusColor(status?: string) {
    switch (status) {
        case 'PENDING': return 'warning';
        case 'CONFIRMED': return 'info';
        case 'RECEIVED': return 'success';
        case 'CANCELLED': return 'error';
        default: return 'grey';
    }
}

function formatDate(dateStr?: string) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString() + ' ' + new Date(dateStr).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
}

function openCreateDialog() {
    orderForm.value = { 
        feedType: '', 
        quantity: 0, 
        supplierId: attachedSupplier.value?.id ?? '' 
    };
    createDialog.value = true;
}

async function submitOrder() {
    if (!orderForm.value.feedType || !orderForm.value.quantity || !orderForm.value.supplierId) return;
    
    await createMutation.mutateAsync({
        feedType: orderForm.value.feedType,
        quantity: orderForm.value.quantity,
        supplierId: orderForm.value.supplierId
    } as any);
    
    createDialog.value = false;
}

function showMessage(item: FeedOrder) {
    if (!item.id) return;
    selectedOrderId.value = item.id;
    messageDialog.value = true;
}

async function confirmOrder(item: FeedOrder) {
    if (!item.id) return;
    if (confirm('Confirm that the supplier has accepted this order?')) {
        await confirmMutation.mutateAsync(item.id);
    }
}

async function receiveOrder(item: FeedOrder) {
    if (!item.id) return;
    if (confirm('Mark order as received? This will add stock to inventory.')) {
        await receiveMutation.mutateAsync(item.id);
    }
}

async function cancelOrder(item: FeedOrder) {
    if (!item.id) return;
    if (confirm('Are you sure you want to cancel this order?')) {
        await cancelMutation.mutateAsync(item.id);
    }
}
</script>
