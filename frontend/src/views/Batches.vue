<template>
  <v-container fluid>
    <PageHeader
      title="Batches"
      action-label="Start Batch"
      @action="dialog = true"
    />

    <v-card>
      <v-data-table
        :headers="headers"
        :items="batchesWithPondName"
        :sort-by="[{ key: 'stockingDate', order: 'desc' }]"
        class="elevation-1"
      >
        <template v-slot:item.status="{ item }">
          <v-chip :color="getStatusColor(item.status)" size="small">
            {{ item.status }}
          </v-chip>
        </template>
        <template v-slot:item.stockingDate="{ item }">
          {{ new Date(item.stockingDate).toLocaleDateString() }}
        </template>
      </v-data-table>
    </v-card>

    <BatchFormDialog v-model="dialog" @save="saveBatch" />
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import BatchFormDialog from '../components/BatchFormDialog.vue';
import PageHeader from '../components/PageHeader.vue';
import type { Batch } from '../types';

const store = useAquaStore();
const dialog = ref(false);

const headers = [
  { title: 'Pond', key: 'pondName' },
  { title: 'Species', key: 'species' },
  { title: 'Stocking Date', key: 'stockingDate' },
  { title: 'Initial Qty', key: 'initialQuantity' },
  { title: 'Status', key: 'status' },
];

const batchesWithPondName = computed(() => {
  return store.batches.map(batch => {
    const pond = store.getPondById(batch.pondId);
    return {
      ...batch,
      pondName: pond ? pond.name : 'Unknown Pond'
    };
  });
});

function getStatusColor(status: string) {
  switch (status) {
    case 'Active': return 'success';
    case 'Harvested': return 'info';
    case 'Cancelled': return 'error';
    default: return 'grey';
  }
}

function saveBatch(batchData: Omit<Batch, 'id'>) {
  store.addBatch(batchData);
}
</script>
