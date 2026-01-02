<template>
  <v-container fluid>
    <PageHeader 
        title="Feed Orders" 
        :action-label="store.isManager ? 'New Order' : undefined"
        @action="openCreateDialog" 
    >
        <template v-slot:actions v-if="store.isManager">
            <v-btn
                variant="outlined"
                color="secondary"
                prepend-icon="mdi-export"
                class="mr-2"
                @click="exportUnpaidSummary"
            >
                Send Unpaid Summary
            </v-btn>
        </template>
    </PageHeader>

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

             <template v-slot:item.confirmationDate="{ item }">
                {{ formatDate(item.confirmationDate) }}
            </template>

             <template v-slot:item.receivedDate="{ item }">
                {{ formatDate(item.receivedDate) }}
            </template>
            
            <template v-slot:item.supplierId="{ item }">
                {{ getSupplierName(item.supplierId) }}
            </template>

            <template v-slot:item.status="{ item }">
                <v-chip :color="getStatusColor(item.status)" size="small" label>
                    {{ item.status }}
                </v-chip>
            </template>

            <template v-slot:item.paymentStatus="{ item }">
                <v-select
                    :model-value="item.paymentStatus"
                    :items="['UNPAID', 'PAID', 'PARTIAL']"
                    density="compact"
                    variant="plain"
                    hide-details
                    class="payment-select"
                    :color="getPaymentColor(item.paymentStatus)"
                    @update:model-value="(val) => updatePaymentStatus(item, val)"
                >
                    <template v-slot:selection="{ item: selItem }">
                         <span :class="`text-caption font-weight-bold text-${getPaymentColor(selItem.value)}`">
                            {{ selItem.value }}
                         </span>
                    </template>
                </v-select>
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

const store = useAquaStore();

const headers = [
  { title: 'Order Date', key: 'orderDate' },
  { title: 'Confirmed', key: 'confirmationDate' },
  { title: 'Received', key: 'receivedDate' },
  { title: 'Feed Type', key: 'feedType' },
  { title: 'kg', key: 'quantity' },
  { title: 'Supplier', key: 'supplierId' },
  { title: 'Status', key: 'status' },
  { title: 'Payment', key: 'paymentStatus' },
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
const updatePaymentMutation = feedOrderService.useUpdatePaymentStatus();

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

function getPaymentColor(status?: string) {
    switch (status) {
        case 'UNPAID': return 'error';
        case 'PAID': return 'success';
        case 'PARTIAL': return 'warning';
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

async function updatePaymentStatus(item: FeedOrder, status: string) {
    if (!item.id) return;
    await updatePaymentMutation.mutateAsync({ id: item.id, status });
}

function exportUnpaidSummary() {
    if (!orders.value || !suppliers.value) return;

    // Group unpaid orders by supplier
    const unpaidBySupplier: Record<string, FeedOrder[]> = {};
    orders.value
        .filter(o => o.paymentStatus !== 'PAID' && o.status !== 'CANCELLED')
        .forEach(o => {
            const sId = o.supplierId;
            if (sId) {
                if (!unpaidBySupplier[sId]) unpaidBySupplier[sId] = [];
                unpaidBySupplier[sId].push(o);
            }
        });

    const supplierIds = Object.keys(unpaidBySupplier);
    if (supplierIds.length === 0) {
        alert('No unpaid orders found!');
        return;
    }

    // Generate summary for each supplier and offer to send
    supplierIds.forEach(sId => {
        const sName = getSupplierName(sId);
        const sOrders = unpaidBySupplier[sId];
        if (!sOrders) return;

        const supplier = suppliers.value?.find(s => s.id === sId);
        const phone = supplier?.whatsappNumber || supplier?.phone;

        if (!phone) {
            console.warn(`No phone number for supplier ${sName}`);
            return;
        }
        
        let summary = `*Unpaid Orders Summary - ${sName}*\n\n`;
        let totalKg = 0;
        sOrders.forEach((o: FeedOrder) => {
            summary += `- ${o.feedType}: ${o.quantity}kg (${formatDate(o.orderDate)})\n`;
            totalKg += (o.quantity || 0);
        });
        summary += `\n*Total Pending:* ${totalKg}kg`;

        if (confirm(`Send unpaid summary to ${sName} (${phone})?`)) {
            const link = `https://web.whatsapp.com/send?phone=${phone.replace(/\+/g, '').replace(/\s/g, '')}&text=${encodeURIComponent(summary)}`;
            window.open(link, '_blank');
        }
    });
}
</script>
