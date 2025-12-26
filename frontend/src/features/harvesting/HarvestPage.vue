<template>
  <v-container fluid>
    <PageHeader title="Harvest & Output Tracking" />

    <v-row>
      <!-- Harvest Entry Form -->
      <v-col v-if="store.isManager" cols="12" lg="4">
        <v-card variant="outlined" class="rounded-lg">
          <v-card-title class="bg-slate-50 border-b px-6 py-4">
              <v-icon start icon="mdi-scale-balance" color="primary"></v-icon>
              Record Daily Harvest
          </v-card-title>
          <v-card-text class="pt-6">
            <v-alert type="info" variant="tonal" class="mb-6" density="compact">
              Biomass below 1KG/pc uses sliding scale pricing. A 5% discount is applied to total weight.
            </v-alert>

            <v-form @submit.prevent="save">
              <v-select
                v-model="form.batchId"
                :items="activeBatches"
                item-title="label"
                item-value="id"
                label="Select Batch"
                variant="outlined"
                density="compact"
                required
              ></v-select>

              <v-row dense v-if="form.batchId">
                <v-col cols="12" sm="6">
                  <v-select
                    v-model="form.fishType"
                    :items="availableSpecies"
                    label="Fish Type"
                    variant="outlined"
                    density="compact"
                    placeholder="Select Species"
                    required
                  ></v-select>
                </v-col>
                <v-col cols="12" sm="6">
                  <v-select
                    v-model="form.pondId"
                    :items="availablePonds"
                    item-title="name"
                    item-value="id"
                    label="Select Pond"
                    variant="outlined"
                    density="compact"
                    required
                  ></v-select>
                </v-col>
              </v-row>

              <v-row dense>
                  <v-col cols="12" sm="6">
                    <v-text-field
                        v-model="form.date"
                        label="Harvest Date"
                        type="date"
                        variant="outlined"
                        density="compact"
                        required
                    ></v-text-field>
                  </v-col>
                  <v-col cols="12" sm="4">
                    <v-text-field
                        v-model.number="form.basicRate"
                        label="Basic Rate (₹/kg)"
                        type="number"
                        variant="outlined"
                        density="compact"
                        required
                        prefix="₹"
                    ></v-text-field>
                  </v-col>
                  <v-col cols="12" sm="4">
                    <v-text-field
                        v-model.number="form.commissionRate"
                        label="Commission Rate (%)"
                        type="number"
                        step="0.01"
                        variant="outlined"
                        density="compact"
                        suffix="%"
                    ></v-text-field>
                  </v-col>
              </v-row>

              <div class="bg-grey-lighten-4 pa-4 rounded-lg mb-4">
                  <v-row dense>
                    <v-col cols="12" sm="6">
                        <v-text-field
                            v-model.number="form.biomassWeight"
                            label="Tonne (Total KG)"
                            type="number"
                            variant="outlined"
                            density="compact"
                            bg-color="white"
                            required
                        ></v-text-field>
                    </v-col>
                    <v-col cols="12" sm="6">
                        <v-text-field
                            v-model.number="form.count"
                            label="Count (Pieces)"
                            type="number"
                            variant="outlined"
                            density="compact"
                            bg-color="white"
                            required
                        ></v-text-field>
                    </v-col>
                  </v-row>

                  <!-- Computed Preview -->
                  <div class="mt-2 text-caption">
                      <div class="d-flex justify-space-between mb-1">
                          <span>Average Weight:</span>
                          <span class="font-weight-bold">{{ calculated.avgWeightGrams.toFixed(2) }}g ({{ (calculated.avgWeightGrams/1000).toFixed(3) }} KG)</span>
                      </div>
                      <div class="d-flex justify-space-between mb-1">
                          <span>Applicable Rate:</span>
                          <span class="font-weight-bold text-primary">₹{{ calculated.finalRate.toFixed(2) }} /kg</span>
                      </div>
                      <div class="d-flex justify-space-between mb-1">
                          <span>Discount (5%):</span>
                          <span class="text-error">-{{ calculated.discountWeight.toFixed(1) }} KG</span>
                      </div>
                      <div class="d-flex justify-space-between mb-1">
                          <span>Commission ({{ form.commissionRate }}%):</span>
                          <span class="text-error">-₹{{ calculated.commission.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</span>
                      </div>
                      <v-divider class="my-2"></v-divider>
                      <div class="d-flex justify-space-between text-subtitle-1">
                          <span class="font-weight-bold">Net Amount:</span>
                          <span class="font-weight-bold text-success">₹{{ calculated.revenue.toLocaleString('en-IN', { minimumFractionDigits: 2 }) }}</span>
                      </div>
                  </div>
              </div>

               <v-text-field
                v-model="form.buyer"
                label="Buyer Name"
                variant="outlined"
                density="compact"
                placeholder="e.g. Oceanic Exports"
              ></v-text-field>

              <v-checkbox
                v-model="isFinal"
                label="Mark as Final Harvest (Closes Batch)"
                hide-details
                density="compact"
                color="primary"
              ></v-checkbox>

              <v-btn color="success" type="submit" block class="mt-6" size="large" :disabled="!isValid">
                Record {{ isFinal ? 'Final' : 'Daily' }} Harvest
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Detailed Harvest Table -->
      <v-col cols="12" :lg="store.isManager ? 8 : 12">
        <v-card variant="outlined" class="rounded-lg shadow-sm">
            <v-card-title class="bg-slate-50 border-b px-6 py-4 d-flex align-center">
                Detailed Harvest History
                <v-spacer></v-spacer>
                <v-chip size="small" color="primary">All values in KG / ₹</v-chip>
            </v-card-title>
            
            <v-divider></v-divider>
            
            <!-- Filters Row -->
            <v-card-text class="bg-slate-50 border-b py-3 px-6">
                <v-row dense align="center">
                    <v-col cols="12" sm="4">
                        <v-select
                            v-model="batchFilter"
                            :items="batchFilterItems"
                            label="Filter by Batch"
                            variant="outlined"
                            density="compact"
                            hide-details
                            clearable
                            placeholder="All Batches"
                        ></v-select>
                    </v-col>
                    <v-col cols="12" sm="4">
                        <v-select
                            v-model="fishTypeFilter"
                            :items="fishTypeFilterItems"
                            label="Filter by Fish Type"
                            variant="outlined"
                            density="compact"
                            hide-details
                            clearable
                            placeholder="All Types"
                        ></v-select>
                    </v-col>
                </v-row>
            </v-card-text>

            <v-table density="compact" hover class="harvest-table">
            <thead>
              <tr class="bg-grey-lighten-3">
                <th class="text-caption font-weight-bold">Batch</th>
                <th class="text-caption font-weight-bold">Pond</th>
                <th class="text-caption font-weight-bold">Fish Type</th>
                <th class="text-caption font-weight-bold">Count</th>
                <th class="text-caption font-weight-bold">Discount (5%)</th>
                <th class="text-caption font-weight-bold">Tonne (KG)</th>
                <th class="text-caption font-weight-bold">Actual</th>
                <th class="text-caption font-weight-bold">Comm.</th>
                <th class="text-caption font-weight-bold">Net Amount</th>
                <th class="text-caption font-weight-bold">Rate</th>
                <th class="text-caption font-weight-bold">Average</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in harvestHistory" :key="log.id" :class="{'bg-green-lighten-5': log.isFinal}">
                <td class="text-no-wrap">{{ formatDateShort(log.date) }}</td>
                <td class="text-no-wrap text-caption">{{ getBatchName(log.batchId) }}</td>
                <td>{{ getPondName(log.pondId) }}</td>
                <td>{{ log.fishType || '-' }}</td>
                <td>{{ log.count?.toLocaleString() || 0 }}</td>
                <td class="text-error">{{ (log.discountWeight || 0).toFixed(1) }}</td>
                <td class="font-weight-medium">{{ (log.biomassWeight || 0).toLocaleString() }}</td>
                <td class="font-weight-medium">{{ (log.actualWeight || 0).toLocaleString() }}</td>
                <td class="text-error text-caption">₹{{ (log.commission || 0).toLocaleString('en-IN', {maximumFractionDigits: 0}) }}</td>
                <td class="text-success font-weight-bold text-no-wrap">₹{{ (log.revenue || 0).toLocaleString('en-IN', {maximumFractionDigits: 0}) }}</td>
                <td class="text-caption">{{ (log.finalRate || 0).toFixed(2) }}</td>
                <td class="text-caption">{{ ((log.avgWeightGrams || 0)/1000).toFixed(3) }}</td>
              </tr>
            </tbody>
          </v-table>
          <v-card-text v-if="harvestHistory.length === 0" class="text-center py-8 text-grey">
              No harvest records yet.
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useAquaStore } from '../../stores/useAquaStore';
import { useRouter } from 'vue-router';
import PageHeader from '../../components/PageHeader.vue';
import { useBatches } from '../batches/composables/useBatches';
import { useHarvestLogs } from './composables/useHarvestLogs';
import { usePonds } from '../ponds/composables/usePonds';
import type { Batch, HarvestLog } from '../../api/generated/model';

const store = useAquaStore();
const router = useRouter();

// State
const isFinal = ref(false);
const batchFilter = ref<string | null>(null);
const fishTypeFilter = ref<string | null>(null);

// Use composables
const { batches, getBatchById } = useBatches();
const { harvestLogs, recordHarvest } = useHarvestLogs(computed(() => ({
    batchId: batchFilter.value || undefined,
    fishType: fishTypeFilter.value || undefined
})));
const { getPondById } = usePonds();

const activeBatches = computed(() => {
  return batches.value?.filter((b: Batch) => b.status === 'Active').map((b: Batch) => {
    const pondNames = [...new Set(b.fishSeeds?.map(s => getPondById(s.pondId || '')?.name).filter(Boolean))].join(', ') || 'Unknown';
    const speciesList = [...new Set(b.fishSeeds?.map(s => s.type) || [])].join(', ') || 'Unknown';
    return {
      id: b.id,
      label: `${b.name || b.stockingDate} - ${pondNames} (${speciesList})`
    };
  }) || [];
});

const defaultForm = {
  batchId: '',
  fishType: '',
  date: new Date().toISOString().substr(0, 10),
  biomassWeight: 0,
  count: 0,
  basicRate: 108,
  commissionRate: 0.2,
  buyer: '',
  pondId: ''
};

const form = ref({ ...defaultForm });

// Auto-select fish type if only one species exists in batch
watch(() => form.value.batchId, (newBatchId: string) => {
    if (!newBatchId) {
        form.value.fishType = '';
        return;
    }
    const batch = getBatchById(newBatchId);
    if (batch && batch.fishSeeds && batch.fishSeeds.length > 0) {
        const species = [...new Set(batch.fishSeeds.map(s => s.type).filter(Boolean))] as string[];
        if (species.length === 1 && species[0]) {
            form.value.fishType = species[0];
        } else {
            form.value.fishType = '';
        }

        const pondIds = [...new Set((batch.fishSeeds as any[]).map(s => s.pondId).filter(Boolean))];
        if (pondIds.length === 1 && pondIds[0]) {
            form.value.pondId = pondIds[0];
        } else {
            form.value.pondId = '';
        }
    }
});

const availableSpecies = computed(() => {
    if (!form.value.batchId) return [];
    const batch = getBatchById(form.value.batchId);
    let species: string[] = [];
    if (form.value.pondId) {
        species = (batch?.fishSeeds as any[])?.filter(s => s.pondId === form.value.pondId).map(s => s.type || '') || [];
    } else {
        species = batch?.fishSeeds?.map(s => s.type || '') || [];
    }
    return [...new Set(species.filter(Boolean)), 'Mixed/All'];
});

const availablePonds = computed(() => {
    if (!form.value.batchId) return [];
    const batch = getBatchById(form.value.batchId);
    const pondIds = [...new Set((batch?.fishSeeds as any[])?.map(s => s.pondId).filter(Boolean) || [])];
    return pondIds.map(id => getPondById(id || '')).filter(Boolean);
});

const calculated = computed(() => {
    const biomass = form.value.biomassWeight || 0;
    const count = form.value.count || 0;
    const basicRate = form.value.basicRate || 0;

    const avgWeight = count > 0 ? biomass / count : 0;
    const avgWeightGrams = avgWeight * 1000;

    const discountWeight = biomass * 0.05;
    const actualWeight = biomass - discountWeight;

    // Logic: applicableSalePrice(basicPrice, forWeight) { return (basicPrice-(1000-forWeight)*0.02) }
    // Applicable if biomass is above KG? "108 is the rate if the fish biomass is above KG"
    // Usually means average weight above 1KG.
    let finalRate = basicRate;
    if (avgWeightGrams > 0 && avgWeightGrams < 1000) {
        finalRate = basicRate - (1000 - avgWeightGrams) * 0.02;
    }

    const commission = (actualWeight * finalRate) * (form.value.commissionRate / 100);
    const revenue = (actualWeight * finalRate) - commission;

    return {
        avgWeightGrams,
        discountWeight,
        actualWeight,
        finalRate,
        commission,
        revenue
    };
});

const isValid = computed(() => form.value.batchId && form.value.pondId && form.value.biomassWeight > 0 && form.value.count > 0);

const harvestHistory = computed(() => {
    return [...(harvestLogs.value || [])].sort((a: HarvestLog, b: HarvestLog) =>
        new Date(b.date || '').getTime() - new Date(a.date || '').getTime()
    );
});

const batchFilterItems = computed(() => {
    const batchIds = [...new Set((harvestLogs.value || []).map(l => l.batchId))];
    return batchIds.map(id => {
        const batch = getBatchById(id);
        const title = batch ? (batch.name || batch.stockingDate || id) : id;
        return {
            title: String(title),
            value: id as string
        };
    }).sort((a, b) => a.title.localeCompare(b.title));
});

const fishTypeFilterItems = computed(() => {
    const types = [...new Set((harvestLogs.value || []).map(l => l.fishType).filter(Boolean))];
    return types.sort();
});

// getFishType removed as we now use log.fishType stored in the record

function getPondName(pondId?: string) {
    if (!pondId) return '-';
    const pond = getPondById(pondId);
    return pond?.name || pondId;
}

function getBatchName(batchId?: string) {
    if (!batchId) return '-';
    const batch = getBatchById(batchId);
    return batch?.name || batch?.stockingDate || '-';
}

function formatDateShort(dateStr?: string) {
	if (!dateStr) return '-';
	return new Date(dateStr).toLocaleDateString(undefined, {
		year: '2-digit',
		month: 'short',
		day: 'numeric'
	});
}

async function save() {
  const payload = {
      ...form.value,
      batchId: form.value.batchId,
      pondId: form.value.pondId,
      fishType: form.value.fishType,
      date: form.value.date,
      count: form.value.count,
      biomassWeight: form.value.biomassWeight,
      basicRate: form.value.basicRate,
      commissionRate: form.value.commissionRate,
      buyer: form.value.buyer,
      avgWeightGrams: calculated.value.avgWeightGrams,
      finalRate: calculated.value.finalRate,
      discountWeight: calculated.value.discountWeight,
      actualWeight: calculated.value.actualWeight,
      commission: calculated.value.commission,
      revenue: calculated.value.revenue,
      isFinal: isFinal.value
  };

  try {
      await recordHarvest(payload);
      if (isFinal.value) {
          alert('Final Harvest recorded and batch closed.');
          router.push('/finance');
      } else {
          alert('Daily harvest record added.');
          form.value = { ...defaultForm, batchId: form.value.batchId, date: form.value.date, basicRate: form.value.basicRate };
      }
  } catch (err) {
      console.error('Failed to record harvest', err);
      alert('Failed to save harvest record. Please try again.');
  }
}
</script>

<style scoped>
.harvest-table th {
    font-size: 0.75rem !important;
    white-space: nowrap;
}
.harvest-table td {
    font-size: 0.85rem !important;
}
</style>
