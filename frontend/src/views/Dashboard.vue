<template>
  <v-container fluid>
    <PageHeader title="Dashboard" />

    <!-- Summary Cards -->
    <v-row>
      <v-col cols="12" sm="6" md="3">
        <v-card color="primary" variant="tonal">
          <v-card-item>
             <div class="text-overline mb-1">Active Ponds</div>
             <div class="text-h4 mb-1">{{ activePondsCount }}</div>
          </v-card-item>
        </v-card>
      </v-col>
      
      <v-col cols="12" sm="6" md="3">
         <v-card color="success" variant="tonal">
          <v-card-item>
             <div class="text-overline mb-1">Biomass Est.</div>
             <div class="text-h4 mb-1">{{ totalBiomassEst }} kg</div>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
         <v-card color="warning" variant="tonal">
          <v-card-item>
             <div class="text-overline mb-1">Feed Today</div>
             <div class="text-h4 mb-1">{{ feedToday }} kg</div>
          </v-card-item>
        </v-card>
      </v-col>
      
       <v-col cols="12" sm="6" md="3">
         <v-card color="error" variant="tonal">
          <v-card-item>
             <div class="text-overline mb-1">Alerts</div>
             <div class="text-h4 mb-1">2</div>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>

    <v-row class="mt-4">
      <!-- Active Batches List -->
      <v-col cols="12" md="8">
        <v-card>
          <v-card-title>Active Batches Overview</v-card-title>
          <v-table>
            <thead>
              <tr>
                <th>Pond</th>
                <th>Species</th>
                <th>Days Culture</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="batch in activeBatches" :key="batch.id">
                <td>{{ getPondName(batch.pondId) }}</td>
                <td>{{ batch.species }}</td>
                <td>{{ getDaysCulture(batch.stockingDate) }} days</td>
                <td><v-chip color="success" size="small">Good</v-chip></td>
              </tr>
            </tbody>
          </v-table>
           <v-card-text v-if="activeBatches.length === 0" class="text-center text-grey">
            No active batches.
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Alerts / Notifications -->
      <v-col cols="12" md="4">
        <v-card>
          <v-card-title>Recent Alerts</v-card-title>
          <v-list density="compact">
            <v-list-item prepend-icon="mdi-water-alert" title="Low DO in Pond A1" subtitle="30 mins ago" color="error"></v-list-item>
            <v-list-item prepend-icon="mdi-flask-empty-minus-outline" title="Feed stock running low" subtitle="2 hours ago" color="warning"></v-list-item>
          </v-list>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import PageHeader from '../components/PageHeader.vue';

const store = useAquaStore();

const activePondsCount = computed(() => store.activePonds.length);

const activeBatches = computed(() => store.batches.filter(b => b.status === 'Active'));

const feedToday = computed(() => {
  const today = new Date().toISOString().substr(0, 10);
  return store.feedLogs
    .filter(l => l.date === today)
    .reduce((sum, l) => sum + l.amount, 0);
});

// Mock calculation for total biomass estimate based on latest sample logs for active batches
const totalBiomassEst = computed(() => {
   // Simplified logic: Just summing up the (avg weight * quantity) from available data would be complex without properly tracking quantity reduction.
   // Returning a static mock or basic calc for visualization.
   return 1250; 
});

function getPondName(pondId: string) {
  const pond = store.getPondById(pondId);
  return pond ? pond.name : pondId;
}

function getDaysCulture(date: string) {
  const start = new Date(date).getTime();
  const now = Date.now();
  const diff = now - start;
  return Math.floor(diff / (1000 * 60 * 60 * 24));
}
</script>
