<template>
  <v-card>
    <v-card-title>Sampling & Water Quality</v-card-title>
    <v-card-text>
      <v-form @submit.prevent="save">
        <v-row>
           <v-col cols="12" sm="6">
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
          </v-col>
           <v-col cols="12" sm="6">
            <v-text-field
              v-model="form.date"
              label="Date"
              type="date"
              variant="outlined"
              density="compact"
              required
            ></v-text-field>
          </v-col>
        </v-row>

        <v-divider class="mb-4"></v-divider>
        <div class="text-subtitle-2 mb-2">Biometrics</div>
        
        <v-row>
          <v-col cols="6" sm="4">
            <v-text-field
              v-model.number="form.averageWeight"
              label="Avg Weight (g)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="4">
            <v-text-field
              v-model.number="form.averageLength"
              label="Avg Length (cm)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="4">
             <v-text-field
              v-model.number="form.survivalEstimate"
              label="Est. Survival (%)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
        </v-row>

        <v-divider class="mb-4"></v-divider>
        <div class="text-subtitle-2 mb-2">Water Quality</div>

        <v-row>
          <v-col cols="6" sm="3">
             <v-text-field
              v-model.number="form.waterQuality.do"
              label="DO (mg/L)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="3">
             <v-text-field
              v-model.number="form.waterQuality.ph"
              label="pH"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="3">
             <v-text-field
              v-model.number="form.waterQuality.temperature"
              label="Temp (°C)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
           <v-col cols="6" sm="3">
             <v-text-field
              v-model.number="form.waterQuality.ammonia"
              label="Ammonia"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
          <v-col cols="6" sm="3">
             <v-text-field
              v-model.number="form.waterQuality.salinity"
              label="Salinity (ppt)"
              type="number"
              variant="outlined"
              density="compact"
            ></v-text-field>
          </v-col>
        </v-row>

        <v-btn color="primary" type="submit" :disabled="!isValid" class="mt-2">Log Sample</v-btn>
      </v-form>

      <v-divider class="my-4"></v-divider>
      
      <h3 class="text-subtitle-1 mb-2">Recent Samples</h3>
       <v-table density="compact">
        <thead>
          <tr>
            <th>Date</th>
            <th>Batch</th>
            <th>ABW (g)</th>
            <th>DO</th>
            <th>pH</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in recentLogs" :key="log.id">
            <td>{{ log.date }}</td>
            <td>{{ getBatchLabel(log.batchId) }}</td>
            <td>{{ log.averageWeight }}</td>
            <td>{{ log.waterQuality.do }}</td>
            <td>{{ log.waterQuality.ph }}</td>
          </tr>
        </tbody>
      </v-table>

    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';

const store = useAquaStore();

const activeBatches = computed(() => {
  return store.batches.filter(b => b.status === 'Active').map(b => {
    const pond = store.getPondById(b.pondId);
    return {
      id: b.id,
      label: `${pond ? pond.name : 'Unknown'} - ${b.species}`
    };
  });
});

const defaultForm = {
  batchId: '',
  date: new Date().toISOString().substr(0, 10),
  averageWeight: 0,
  averageLength: 0,
  survivalEstimate: 100,
  waterQuality: {
    do: 0,
    ph: 7.5,
    temperature: 28,
    ammonia: 0,
    salinity: 0,
    turbidity: 0
  }
};

const form = ref(JSON.parse(JSON.stringify(defaultForm))); // Deep copy for nested obj

const isValid = computed(() => form.value.batchId);

const recentLogs = computed(() => {
    return [...store.sampleLogs].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()).slice(0, 5);
});

function getBatchLabel(batchId: string) {
     const batch = store.getBatchById(batchId);
    if (!batch) return batchId;
    const pond = store.getPondById(batch.pondId);
    return `${pond ? pond.name : 'Unknown'}`;
}

function save() {
  store.logSample({ ...form.value });
  // Reset fields
  form.value = JSON.parse(JSON.stringify(defaultForm));
}
</script>
