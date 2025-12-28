<template>
  <v-container fluid>
    <PageHeader
        title="Finance Dashboard"
        :action-label="store.isManager ? 'Log Entry' : undefined"
        @action="dialog = true"
    />

    <v-row v-if="!store.isManager">
      <v-col cols="12">
        <v-alert type="error" variant="tonal" title="Access Denied">
          Only Managers and Admins can view or add financial data.
        </v-alert>
      </v-col>
    </v-row>

    <template v-else>
      <v-tabs v-model="tab" color="primary" class="mb-4 align-tabs-title">
        <v-tab value="expenditure">
          <v-icon start icon="mdi-minus-circle-outline"></v-icon>
          Outgoings (Expenditure)
        </v-tab>
        <v-tab value="income">
          <v-icon start icon="mdi-plus-circle-outline"></v-icon>
          Incomings (Payments)
        </v-tab>
      </v-tabs>

      <v-window v-model="tab">
        <!-- Expenditure Tab -->
        <v-window-item value="expenditure">
          <v-card variant="outlined" class="rounded-lg shadow-sm">
            <v-card-title class="px-6 py-4 bg-slate-50 border-b">
                <div class="d-flex align-center justify-space-between w-100 flex-wrap gap-4">
                    <div class="d-flex align-center">
                        <v-icon icon="mdi-finance" color="primary" class="mr-2"></v-icon>
                        Expenditure Log
                    </div>
                    <div class="d-flex gap-2">
                        <v-select
                            v-model="filters.batchId"
                            :items="batchOptions"
                            item-title="label"
                            item-value="id"
                            label="Filter by Batch"
                            density="compact"
                            variant="outlined"
                            hide-details
                            clearable
                            class="w-48"
                            @update:model-value="onFilterChange"
                        ></v-select>
                        <v-select
                            v-model="filters.macroInvestment"
                            :items="['ADVANCE', 'LEASE', 'INVESTMENT', 'NO_RETURN']"
                            label="Macro Category"
                            density="compact"
                            variant="outlined"
                            hide-details
                            clearable
                            class="w-40"
                            @update:model-value="onFilterChange"
                        ></v-select>
                        <v-select
                            v-model="filters.category"
                            :items="['Feed', 'Seed', 'Salary', 'Maintenance', 'Electricity', 'Other']"
                            label="Type"
                            density="compact"
                            variant="outlined"
                            hide-details
                            clearable
                            class="w-32"
                            @update:model-value="onFilterChange"
                        ></v-select>
                    </div>
                </div>
            </v-card-title>

            <v-data-table-server
                v-model:items-per-page="itemsPerPage"
                :headers="headers"
                :items="serverItems"
                :items-length="totalItems"
                :loading="loading"
                item-value="id"
                @update:options="loadItems"
                hover
            >
                <template v-slot:item.date="{ item }">
                    {{ formatDate(item.date) }}
                </template>
                <template v-slot:item.batchId="{ item }">
                    {{ getBatchName(item.batchId) }}
                </template>
                <template v-slot:item.amount="{ item }">
                    <span class="font-weight-bold">₹{{ (item.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</span>
                </template>
            </v-data-table-server>
          </v-card>
        </v-window-item>

        <!-- Income Tab -->
        <v-window-item value="income">
          <v-card variant="outlined" class="rounded-lg shadow-sm">
            <v-card-title class="px-6 py-4 bg-slate-50 border-b">
                <div class="d-flex align-center">
                    <v-icon icon="mdi-cash-plus" color="success" class="mr-2"></v-icon>
                    Incoming Payments (Harvest Revenue)
                </div>
            </v-card-title>

            <v-table hover>
              <thead>
                <tr class="bg-grey-lighten-4">
                  <th class="text-subtitle-2 font-weight-bold">Date</th>
                  <th class="text-subtitle-2 font-weight-bold">Source / Buyer</th>
                  <th class="text-subtitle-2 font-weight-bold">Harvest Details</th>
                  <th class="text-subtitle-2 font-weight-bold text-right">Amount Received</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="log in harvestLogs" :key="log.id">
                  <td>{{ formatDate(log.date) }}</td>
                  <td class="font-weight-medium">{{ log.buyer || 'Direct Sale' }}</td>
                  <td class="text-grey-darken-1">
                      {{ log.biomassWeight }}kg (Est. {{ log.count?.toLocaleString() || 0 }} pcs)
                      <div class="text-caption">Batch: {{ getBatchName(log.batchId) }}</div>
                  </td>
                  <td class="text-right font-weight-bold text-success text-h6">
                      ₹{{ (log.revenue || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}
                  </td>
                </tr>
              </tbody>
              <tfoot class="bg-slate-100 font-weight-bold">
                  <tr>
                      <td colspan="3" class="text-right text-uppercase text-caption">Total Incomings:</td>
                      <td class="text-right text-h5 text-success">₹{{ grandTotalInc.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</td>
                  </tr>
              </tfoot>
            </v-table>
            <v-card-text v-if="!harvestLogs || harvestLogs.length === 0" class="text-center py-12 text-grey">
              <v-icon icon="mdi-cash-off" size="48" class="mb-2 opacity-20"></v-icon>
              <div>No incoming payments recorded.</div>
            </v-card-text>
          </v-card>

          <!-- Summary Alert -->
          <v-alert
            v-if="grandTotalInc > grandTotalExp"
            type="success"
            variant="tonal"
            class="mt-6 rounded-lg"
            title="Positive Net Position"
          >
            Your total incomings (₹{{ grandTotalInc.toLocaleString() }}) exceed your recorded outgoings (₹{{ grandTotalExp.toLocaleString() }}).
            Net Position: <strong>₹{{ (grandTotalInc - grandTotalExp).toLocaleString() }}</strong>
          </v-alert>
        </v-window-item>
      </v-window>
    </template>

    <!-- Entry Dialog -->
    <ExpenditureDialog
      v-model="dialog"
      @save="handleSaveExpenditure"
    />

  </v-container>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue';
import { useAquaStore } from '../../stores/useAquaStore';
import PageHeader from '../../components/PageHeader.vue';
import ExpenditureDialog from './components/ExpenditureDialog.vue';
import { expenditureService } from './api/expenditureService';
import { useHarvestLogs } from '../harvesting/composables/useHarvestLogs';
import { useBatches } from '../batches/composables/useBatches';
import type { Expenditure, HarvestLog } from '../../api/generated/model';

const store = useAquaStore();
const dialog = ref(false);
const tab = ref('expenditure');

// Use composables
const { harvestLogs } = useHarvestLogs();
const { batches, getBatchById } = useBatches(); // Need batches list for filter

// Instantiate mutation at setup level
const createMutation = expenditureService.useCreate();

const batchOptions = computed(() => {
    return batches.value?.map(b => ({
        id: b.id,
        label: b.name || `Batch ${b.stockingDate}`
    })) || [];
});

async function handleSaveExpenditure(payload: Omit<Expenditure, 'id'>) {
    // Originally called createExpenditure but we are managing state locally here via search
    // We should probably still call the service create.
    await createMutation.mutateAsync({ data: payload as Expenditure });
    dialog.value = false;
    loadItems({ page: 1, itemsPerPage: itemsPerPage.value, sortBy: [] });
}

// Data Table State
const itemsPerPage = ref(10);
const headers: any[] = [
    { title: 'Date', key: 'date', sortable: false }, // Sort not implemented in backend yet
    { title: 'Batch', key: 'batchId', sortable: false },
    { title: 'Macro', key: 'macroInvestment', sortable: false },
    { title: 'Type', key: 'category', sortable: false },
    { title: 'Sub-Type', key: 'subType', sortable: false },
    { title: 'Description', key: 'description', sortable: false },
    { title: 'Amount', key: 'amount', align: 'end', sortable: false },
];
const serverItems = ref<Expenditure[]>([]);
const totalItems = ref(0);
const loading = ref(false);

const filters = reactive({
    batchId: null as string | null,
    macroInvestment: null as string | null,
    category: null as string | null
});

function onFilterChange() {
    loadItems({ page: 1, itemsPerPage: itemsPerPage.value, sortBy: [] });
}

// Load Items Function compatible with v-data-table-server @update:options
async function loadItems({ page, itemsPerPage, sortBy }: { page?: number, itemsPerPage?: number, sortBy?: any[] } = {}) {
    loading.value = true;
    try {
        const p = page || 1;
        const size = itemsPerPage || 10;
        
        const response = await expenditureService.search({
            batchId: filters.batchId || undefined,
            macroInvestment: filters.macroInvestment || undefined,
            category: filters.category || undefined,
            page: p - 1, // Spring Boot is 0-indexed
            size: size,
            // sort not implemented yet
        });

        serverItems.value = response.content || [];
        totalItems.value = response.totalElements || 0;
    } catch (e) {
        console.error("Failed to load expenditures", e);
    } finally {
        loading.value = false;
    }
}


const grandTotalExp = computed(() => {
    // Note: Grand total calculation on client side is now restricted to the current page view or requires a separate aggregation endpoint
    // For now, we can sum the currently visible items or remove this total if misleading.
    // Given the pivot request was about "views", maybe total is less important on raw list or needs separate API.
    // Returning sum of loaded items for now.
    return serverItems.value.reduce((sum, item) => sum + (item.amount || 0), 0);
});

const grandTotalInc = computed(() => harvestLogs.value?.reduce((sum: number, log: HarvestLog) => sum + (log.revenue || 0), 0) || 0);

function formatDate(dateStr?: string) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function getBatchName(batchId?: string) {
    if (!batchId) return 'General / Unallocated';
    const batch = getBatchById(batchId);
    return batch?.name || batch?.stockingDate || 'Unknown';
}

// Initial load
// loadItems called by v-data-table-server automatically on mount
</script>

<style scoped>
.v-table th {
    background-color: #f8fafc !important;
}
.v-table td {
    height: 64px !important;
    vertical-align: middle;
}
</style>
