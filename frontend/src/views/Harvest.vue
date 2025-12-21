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
                    <v-select
                        v-model.number="form.commissionRate"
                        :items="[0.2, 0.25, 0.3]"
                        label="Commission Rate (%)"
                        variant="outlined"
                        density="compact"
                        suffix="%"
                    ></v-select>
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
            <v-table density="compact" hover class="harvest-table">
            <thead>
              <tr class="bg-grey-lighten-3">
                <th class="text-caption font-weight-bold">Date</th>
                <th class="text-caption font-weight-bold">Fish Type</th>
                <th class="text-caption font-weight-bold">Count</th>
                <th class="text-caption font-weight-bold">Discount (5%)</th>
                <th class="text-caption font-weight-bold">Tonne (KG)</th>
                <th class="text-caption font-weight-bold">Actual</th>
                <th class="text-caption font-weight-bold">Comm.</th>
                <th class="text-caption font-weight-bold">Net Amount</th>
                <th class="text-caption font-weight-bold">Rate</th>
                <th class="text-caption font-weight-bold">Average</th>
                <th class="text-caption font-weight-bold">Batch</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in harvestHistory" :key="log.id" :class="{'bg-green-lighten-5': log.isFinal}">
                <td class="text-no-wrap">{{ formatDateShort(log.date) }}</td>
                <td>{{ getFishType(log.batchId) }}</td>
                <td>{{ log.count.toLocaleString() }}</td>
                <td class="text-error">{{ log.discountWeight.toFixed(1) }}</td>
                <td class="font-weight-medium">{{ log.biomassWeight.toLocaleString() }}</td>
                <td class="font-weight-medium">{{ log.actualWeight.toLocaleString() }}</td>
                <td class="text-error text-caption">₹{{ (log.commission || 0).toLocaleString('en-IN', {maximumFractionDigits: 0}) }}</td>
                <td class="text-success font-weight-bold text-no-wrap">₹{{ log.revenue.toLocaleString('en-IN', {maximumFractionDigits: 0}) }}</td>
                <td class="text-caption">{{ log.finalRate.toFixed(2) }}</td>
                <td class="text-caption">{{ (log.avgWeightGrams/1000).toFixed(3) }}</td>
                <td class="text-no-wrap text-caption">{{ getBatchName(log.batchId) }}</td>
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
import { useAquaStore } from '../stores/useAquaStore';
import { useRouter } from 'vue-router';
import PageHeader from '../components/PageHeader.vue';

const store = useAquaStore();
const router = useRouter();

const isFinal = ref(false);

const activeBatches = computed(() => {
  return store.batches.filter(b => b.status === 'Active').map(b => {
    const pond = store.getPondById(b.pondId);
    return {
      id: b.id,
      label: `${b.name || b.stockingDate} - ${pond ? pond.name : 'Unknown'} (${b.species})`
    };
  });
});

const defaultForm = {
  batchId: '',
  date: new Date().toISOString().substr(0, 10),
  biomassWeight: 0,
  count: 0,
  basicRate: 108,
  commissionRate: 0.2,
  buyer: ''
};

const form = ref({ ...defaultForm });

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

const isValid = computed(() => form.value.batchId && form.value.biomassWeight > 0 && form.value.count > 0);

const harvestHistory = computed(() => {
    return [...store.harvestLogs].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
});

function getFishType(batchId: string) {
    const batch = store.getBatchById(batchId);
    return batch ? batch.species : '-';
}

function getBatchName(batchId: string) {
    const batch = store.getBatchById(batchId);
    return batch ? (batch.name || batch.stockingDate) : '-';
}

function formatDateShort(dateStr: string) {
    return new Date(dateStr).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: '20' === dateStr.slice(0,2) ? '2-digit' : 'numeric' });
}

function save() {
  const payload = {
      ...form.value,
      avgWeightGrams: calculated.value.avgWeightGrams,
      finalRate: calculated.value.finalRate,
      discountWeight: calculated.value.discountWeight,
      actualWeight: calculated.value.actualWeight,
      commission: calculated.value.commission,
      revenue: calculated.value.revenue,
      isFinal: isFinal.value
  };

  store.recordHarvest(payload, isFinal.value);
  
  if (isFinal.value) {
      alert('Final Harvest recorded and batch closed.');
      router.push('/finance'); 
  } else {
      alert('Daily harvest record added.');
      form.value = { ...defaultForm, batchId: form.value.batchId, date: form.value.date, basicRate: form.value.basicRate };
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
