<template>
  <v-container fluid>
    <PageHeader title="Dashboard" />

    <!-- Summary Cards -->
    <v-row>
      <v-col cols="12" sm="6" md="3">
        <v-card color="primary" variant="tonal" class="rounded-lg">
          <v-card-item>
             <div class="text-overline mb-1">Active Ponds</div>
             <div class="text-h4 mb-1 font-weight-bold">{{ activePondsCount }}</div>
          </v-card-item>
        </v-card>
      </v-col>
      
      <v-col cols="12" sm="6" md="3">
         <v-card color="success" variant="tonal" class="rounded-lg">
          <v-card-item>
             <div class="text-overline mb-1">Biomass Est.</div>
             <div class="text-h4 mb-1 font-weight-bold">{{ totalBiomassEst }} kg</div>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
         <v-card color="warning" variant="tonal" class="rounded-lg">
          <v-card-item>
             <div class="text-overline mb-1">Feed Today</div>
             <div class="text-h4 mb-1 font-weight-bold">{{ feedToday }} kg</div>
          </v-card-item>
        </v-card>
      </v-col>
      
       <v-col cols="12" sm="6" md="3">
         <v-card color="error" variant="tonal" class="rounded-lg">
          <v-card-item>
             <div class="text-overline mb-1">Alerts</div>
             <div class="text-h4 mb-1 font-weight-bold">2</div>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>

    <!-- Running Batches Section -->
    <v-row class="mt-6">
      <v-col cols="12">
        <v-card variant="outlined" class="rounded-lg">
          <v-card-title class="px-6 py-4 d-flex align-center flex-wrap">
            <v-icon icon="mdi-calendar-clock" class="mr-2" color="primary"></v-icon>
            <span class="mr-4">Running Batches Timeline</span>
            <v-spacer></v-spacer>
            <div style="width: 250px;">
                <v-text-field
                  v-model="referenceDate"
                  label="View age at date"
                  type="date"
                  variant="outlined"
                  density="compact"
                  hide-details
                  prepend-inner-icon="mdi-calendar-search"
                ></v-text-field>
            </div>
          </v-card-title>
          <v-divider></v-divider>
          <v-table hover>
            <thead>
              <tr class="bg-grey-lighten-4">
                <th class="text-subtitle-2 font-weight-bold">Batch</th>
                <th class="text-subtitle-2 font-weight-bold">Pond</th>
                <th class="text-subtitle-2 font-weight-bold">Start Date</th>
                <th class="text-subtitle-2 font-weight-bold">Est. End Date</th>
                <th class="text-subtitle-2 font-weight-bold text-center">Days of Culture</th>
                <th class="text-subtitle-2 font-weight-bold">Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="batch in activeBatchesWithHistory" :key="batch.id">
                <td class="font-weight-medium text-primary">{{ batch.name || 'Unnamed' }}</td>
                <td>{{ getPondName(batch.pondId) }}</td>
                <td>{{ formatDate(batch.stockingDate) }}</td>
                <td>{{ batch.estimatedHarvestDate ? formatDate(batch.estimatedHarvestDate) : '-' }}</td>
                <td class="text-center">
                  <v-chip size="small" variant="tonal" :color="getAgeColor(batch.stockingDate)">
                    {{ calculateAge(batch.stockingDate) }} Days
                  </v-chip>
                </td>
                <td>
                  <v-chip 
                    :color="getBatchStatusColor(batch.stockingDate)" 
                    size="x-small" 
                    variant="flat"
                    class="text-uppercase"
                  >
                    {{ getBatchStatusLabel(batch.stockingDate) }}
                  </v-chip>
                </td>
              </tr>
            </tbody>
          </v-table>
          <v-card-text v-if="activeBatches.length === 0" class="text-center py-8 text-grey">
            <v-icon icon="mdi-history" size="48" class="mb-2 opacity-20"></v-icon>
            <div>No running batches.</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-row class="mt-4">
      <!-- Alerts / Notifications -->
      <v-col cols="12" md="6">
        <v-card variant="outlined" class="rounded-lg">
          <v-card-title class="px-6 py-4">Recent Alerts</v-card-title>
          <v-divider></v-divider>
          <v-list density="comfortable">
            <v-list-item prepend-icon="mdi-water-alert" title="Low DO in Pond A1" subtitle="30 mins ago" color="error"></v-list-item>
            <v-list-item prepend-icon="mdi-flask-empty-minus-outline" title="Feed stock running low" subtitle="2 hours ago" color="warning"></v-list-item>
          </v-list>
        </v-card>
      </v-col>
      
      <!-- Quick Stats / Forecast -->
      <v-col cols="12" md="6">
        <v-card variant="outlined" class="rounded-lg">
          <v-card-title class="px-6 py-4">Harvest Forecast</v-card-title>
          <v-divider></v-divider>
          <v-card-text>
              <div class="d-flex align-center justify-space-between mb-4">
                  <span class="text-grey-darken-1">Next Expected Harvest:</span>
                  <span class="font-weight-bold">{{ formatDate('2025-01-23') }}</span>
              </div>
              <v-progress-linear model-value="85" color="success" height="12" rounded></v-progress-linear>
              <div class="text-caption mt-2 text-right">85% through the current cycle</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import PageHeader from '../components/PageHeader.vue';

const store = useAquaStore();

const referenceDate = ref(new Date().toISOString().substr(0, 10));

const activePondsCount = computed(() => store.activePonds.length);

const activeBatches = computed(() => store.batches.filter(b => b.status === 'Active'));

// Sort batches to show the ones coming up
const activeBatchesWithHistory = computed(() => {
    return [...activeBatches.value].sort((a, b) => new Date(a.stockingDate).getTime() - new Date(b.stockingDate).getTime());
});

const feedToday = computed(() => {
  const today = new Date().toISOString().substr(0, 10);
  return store.feedLogs
    .filter(l => l.date === today)
    .reduce((sum, l) => sum + l.amount, 0);
});

const totalBiomassEst = computed(() => 1250);

function getPondName(pondId: string) {
  const pond = store.getPondById(pondId);
  return pond ? pond.name : pondId;
}

function formatDate(dateStr: string) {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateStr).toLocaleDateString(undefined, options);
}

function calculateAge(stockingDate: string) {
  const start = new Date(stockingDate).setHours(0,0,0,0);
  const refDate = new Date(referenceDate.value).setHours(0,0,0,0);
  
  if (refDate < start) return 0; // Hasn't started yet relative to selection
  
  const diff = refDate - start;
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  return days;
}

function getAgeColor(stockingDate: string) {
    const age = calculateAge(stockingDate);
    if (age === 0) return 'grey';
    if (age < 30) return 'cyan';
    if (age < 90) return 'blue';
    return 'indigo';
}

function getBatchStatusLabel(stockingDate: string) {
    const start = new Date(stockingDate).getTime();
    const refDate = new Date(referenceDate.value).getTime();
    if (start > refDate) return 'Scheduled';
    return 'Running';
}

function getBatchStatusColor(stockingDate: string) {
    const start = new Date(stockingDate).getTime();
    const refDate = new Date(referenceDate.value).getTime();
    if (start > refDate) return 'info';
    return 'success';
}
</script>
