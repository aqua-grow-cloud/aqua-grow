<template>
  <v-container fluid>
    <PageHeader title="Harvest & Output" />

    <v-row>
      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>Record Harvest</v-card-title>
          <v-card-text>
            <v-alert type="info" variant="tonal" class="mb-4" density="compact">
              Recording a harvest will automatically close the selected Batch and set the Pond to Inactive.
            </v-alert>

            <v-form @submit.prevent="save">
              <v-select
                v-model="form.batchId"
                :items="activeBatches"
                item-title="label"
                item-value="id"
                label="Select Batch to Harvest"
                variant="outlined"
                density="compact"
                required
              ></v-select>

              <v-text-field
                v-model="form.date"
                label="Harvest Date"
                type="date"
                variant="outlined"
                density="compact"
                required
              ></v-text-field>

              <v-text-field
                v-model.number="form.biomassWeight"
                label="Total Biomass (kg)"
                type="number"
                variant="outlined"
                density="compact"
                required
              ></v-text-field>

              <v-text-field
                v-model.number="form.count"
                label="Total Count (pieces)"
                type="number"
                variant="outlined"
                density="compact"
                required
              ></v-text-field>

              <v-text-field
                v-model.number="form.revenue"
                label="Total Revenue ($)"
                type="number"
                variant="outlined"
                density="compact"
                required
              ></v-text-field>

               <v-text-field
                v-model="form.buyer"
                label="Buyer Name (Optional)"
                variant="outlined"
                density="compact"
              ></v-text-field>

              <v-btn color="success" type="submit" block class="mt-4" :disabled="!isValid">
                Record Final Harvest
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="6">
        <v-card>
            <v-card-title>Harvest History</v-card-title>
            <v-table density="compact">
            <thead>
              <tr>
                <th>Date</th>
                <th>Batch</th>
                <th>Biomass</th>
                <th>Revenue</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in harvestHistory" :key="log.id">
                <td>{{ log.date }}</td>
                <td>{{ getBatchLabel(log.batchId) }}</td>
                <td>{{ log.biomassWeight.toLocaleString() }} kg</td>
                <td>${{ log.revenue.toLocaleString() }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import { useRouter } from 'vue-router';
import PageHeader from '../components/PageHeader.vue';

const store = useAquaStore();
const router = useRouter();

const activeBatches = computed(() => {
  return store.batches.filter(b => b.status === 'Active').map(b => {
    const pond = store.getPondById(b.pondId);
    return {
      id: b.id,
      label: `${pond ? pond.name : 'Unknown'} (${b.species})`
    };
  });
});

const defaultForm = {
  batchId: '',
  date: new Date().toISOString().substr(0, 10),
  biomassWeight: 0,
  count: 0,
  revenue: 0,
  buyer: ''
};

const form = ref({ ...defaultForm });

const isValid = computed(() => form.value.batchId && form.value.biomassWeight > 0);

const harvestHistory = computed(() => {
    return [...store.harvestLogs].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
});

function getBatchLabel(batchId: string) { // Helper function, should probably be shared but duplicated for speed now
     const batch = store.getBatchById(batchId);
    if (!batch) return batchId;
    // For Harvest history, even if batch is inactive, we can still show species.
    return batch.species; // Simplified for history
}

function save() {
  if (confirm('This will close the batch. Continue?')) {
    store.recordHarvest({ ...form.value });
    alert('Harvest recorded and batch closed.');
    form.value = { ...defaultForm };
    router.push('/ponds'); // Redirect to ponds list
  }
}
</script>
