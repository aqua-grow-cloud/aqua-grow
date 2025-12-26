<template>
  <v-container fluid>
    <PageHeader
        title="Feed Inventory"
        action-label="Add Feed Stock"
        @action="openDialog"
    />

    <v-row>
        <v-col cols="12">
             <v-card variant="outlined" class="rounded-lg mt-4">
                <v-data-table
                    :headers="headers"
                    :items="feedStocks || []"
                    :loading="isLoading"
                    :sort-by="[{ key: 'lastStockingDate', order: 'desc' }]"
                    class="elevation-0"
                >
                    <template v-slot:item.lastStockingDate="{ item }">
                        {{ item.lastStockingDate ? new Date(item.lastStockingDate).toLocaleDateString() : 'Never' }}
                    </template>
                    <template v-slot:item.currentStock="{ item }">
                        <div class="d-flex align-center">
                            <v-badge
                                :color="getStockColor(item)"
                                dot
                                inline
                                class="mr-2"
                            ></v-badge>
                            <span class="font-weight-bold">{{ item.currentStock }} {{ item.unit }}</span>
                        </div>
                    </template>

                    <template v-slot:item.status="{ item }">
                        <v-chip :color="getStockColor(item)" size="small" variant="flat">
                            {{ getStockStatus(item) }}
                        </v-chip>
                    </template>

                    <template v-slot:item.actions="{ item }">
                        <v-tooltip text="Update Stock">
                            <template v-slot:activator="{ props }">
                                <v-btn v-bind="props" icon size="small" variant="text" color="primary" @click="editItem(item)">
                                    <v-icon>mdi-pencil</v-icon>
                                </v-btn>
                            </template>
                        </v-tooltip>
                         <v-tooltip text="Order More">
                            <template v-slot:activator="{ props }">
                                <v-btn
                                    v-bind="props"
                                    icon
                                    size="small"
                                    variant="text"
                                    color="success"
                                    @click="quickOrder(item)"
                                    :disabled="!item.supplierId"
                                >
                                    <v-icon>mdi-cart-plus</v-icon>
                                </v-btn>
                            </template>
                        </v-tooltip>
                         <v-btn icon size="small" variant="text" color="error" @click="deleteItem(item)">
                            <v-icon>mdi-delete</v-icon>
                        </v-btn>
                    </template>
                </v-data-table>
            </v-card>
        </v-col>
    </v-row>

    <!-- Stock Form Dialog -->
    <v-dialog v-model="dialog" max-width="500px">
      <v-card>
        <v-card-title>
          <span class="text-h5">{{ form.id ? 'Edit Stock' : 'New Feed Stock' }}</span>
        </v-card-title>
        <v-card-text>
          <v-container>
            <v-form ref="formRef" @submit.prevent="save">
                <v-row>
                     <v-col cols="12">
                        <v-text-field
                        v-model="form.feedType"
                        label="Feed Type"
                        variant="outlined"
                        density="compact"
                        required
                        ></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="6">
                        <v-text-field
                        v-model.number="form.currentStock"
                        label="Current Stock"
                        type="number"
                        variant="outlined"
                        density="compact"
                        required
                        ></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="6">
                        <v-text-field
                        v-model="form.unit"
                        label="Unit"
                        variant="outlined"
                        density="compact"
                        placeholder="kg, bags"
                        required
                        ></v-text-field>
                    </v-col>
                     <v-col cols="12" sm="6">
                        <v-text-field
                        v-model.number="form.threshold"
                        label="Low Stock Threshold"
                        type="number"
                        variant="outlined"
                        density="compact"
                        required
                        ></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="6">
                        <v-text-field
                        v-model="form.lastStockingDate"
                        label="Stocking Date"
                        type="date"
                        variant="outlined"
                        density="compact"
                        ></v-text-field>
                    </v-col>
                    <v-col cols="12">
                        <v-select
                            v-model="form.supplierId"
                            :items="suppliers || []"
                            item-title="name"
                            item-value="id"
                            label="Default Supplier"
                            variant="outlined"
                            density="compact"
                            clearable
                        ></v-select>
                    </v-col>
                </v-row>
            </v-form>
          </v-container>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="blue-darken-1" variant="text" @click="close">Cancel</v-btn>
          <v-btn color="blue-darken-1" variant="text" @click="save">Save</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Order Dialog (Quick access) -->
    <v-dialog v-model="orderDialog" max-width="500px">
        <v-card>
            <v-card-title>Create Order</v-card-title>
            <v-card-text>
                <v-form @submit.prevent="submitOrder">
                     <v-text-field
                        v-model="orderForm.feedType"
                        label="Feed Type"
                        variant="outlined"
                        readonly
                        density="compact"
                    ></v-text-field>
                     <v-text-field
                        v-model.number="orderForm.quantity"
                        label="Quantity"
                        type="number"
                        variant="outlined"
                        density="compact"
                        :suffix="orderForm.unit"
                        autofocus
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
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAquaStore } from '../../stores/useAquaStore';
import PageHeader from '../../components/PageHeader.vue';
import { feedStockService } from './api/feedStockService';
import { supplierService } from './api/supplierService';
import { feedOrderService } from './api/feedOrderService';
import type { FeedStock } from '../../api/manual/types';
import { useRouter } from 'vue-router';

const router = useRouter();

const headers = [
  { title: 'Feed Type', key: 'feedType' },
  { title: 'Current Stock', key: 'currentStock' },
  { title: 'Threshold', key: 'threshold' },
  { title: 'Last Stocked', key: 'lastStockingDate' },
  { title: 'Status', key: 'status' },
  { title: 'Actions', key: 'actions', sortable: false },
];

const { data: feedStocks, isLoading } = feedStockService.useGetAll();
const { data: suppliers } = supplierService.useGetAll();
const createMutation = feedStockService.useCreate();
const updateMutation = feedStockService.useUpdate();
const deleteMutation = feedStockService.useDelete();
const createOrderMutation = feedOrderService.useCreate();

const dialog = ref(false);
const orderDialog = ref(false);

const defaultForm: FeedStock = {
    feedType: '',
    currentStock: 0,
    threshold: 100,
    unit: 'kg',
    supplierId: '',
    lastStockingDate: new Date().toISOString().substr(0, 10)
};
const form = ref<FeedStock>({ ...defaultForm });

const orderForm = ref({
    feedType: '',
    quantity: 0,
    supplierId: '',
    unit: ''
});

function getStockColor(item: FeedStock) {
    if (item.currentStock <= item.threshold) return 'error';
    if (item.currentStock <= item.threshold * 1.5) return 'warning';
    return 'success';
}

function getStockStatus(item: FeedStock) {
     if (item.currentStock <= item.threshold) return 'Low Stock';
    if (item.currentStock <= item.threshold * 1.5) return 'Reorder Soon';
    return 'Good';
}

function openDialog() {
    form.value = { ...defaultForm };
    dialog.value = true;
}

function editItem(item: FeedStock) {
    form.value = { 
        ...item,
        lastStockingDate: item.lastStockingDate ? item.lastStockingDate.substr(0, 10) : undefined
    };
    dialog.value = true;
}

async function deleteItem(item: FeedStock) {
    if (!item.id) return;
    if (confirm(`Are you sure you want to delete ${item.feedType} stock?`)) {
        await deleteMutation.mutateAsync(item.id);
    }
}

function close() {
    dialog.value = false;
}

async function save() {
    if (!form.value.feedType) return;

    if (form.value.id) {
        await updateMutation.mutateAsync({ id: form.value.id, data: form.value });
    } else {
        await createMutation.mutateAsync(form.value);
    }
    close();
}

function quickOrder(item: FeedStock) {
    if (!item.supplierId) {
        alert('Please update stock with a default supplier first.');
        return;
    }
    orderForm.value = {
        feedType: item.feedType,
        quantity: item.threshold * 2, // Suggest ordering enough to double threshold
        supplierId: item.supplierId,
        unit: item.unit
    };
    orderDialog.value = true;
}

async function submitOrder() {
    if (!orderForm.value.quantity || !orderForm.value.supplierId) return;

    await createOrderMutation.mutateAsync({
        feedType: orderForm.value.feedType,
        quantity: orderForm.value.quantity,
        supplierId: orderForm.value.supplierId
        // Status and date handled by backend defaults
    } as any);

    orderDialog.value = false;
    router.push('/feed-orders'); // Redirect to orders to send message
}
</script>
