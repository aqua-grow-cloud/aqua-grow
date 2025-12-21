<template>
  <v-container fluid>
    <PageHeader 
      title="Ponds" 
      :action-label="store.isManager ? 'Add Pond' : undefined" 
      @action="openCreateDialog" 
    />

    <v-row>
      <v-col v-for="pond in store.ponds" :key="pond.id" cols="12" sm="6" md="4" lg="3">
        <PondCard 
          :pond="pond" 
          :can-manage="store.isManager"
          @edit="openEditDialog" 
          @delete="deletePond"
          @stock="openStockDialog"
        />
      </v-col>
    </v-row>

    <!-- Empty State -->
    <v-alert
      v-if="store.ponds.length === 0"
      type="info"
      variant="tonal"
      class="mt-4"
    >
      No ponds found. Create one to get started.
    </v-alert>

    <PondFormDialog
      v-model="dialog"
      :item="editedItem"
      @save="savePond"
    />
    
    <BatchFormDialog 
        v-model="stockDialog" 
        :initial-pond-id="stockPondId"
        @save="saveBatch"
        @add-seed="handleAddSeed"
    />
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import type { Pond, Batch, FishSeed } from '../types';
import PondCard from '../components/PondCard.vue';
import PondFormDialog from '../components/PondFormDialog.vue';
import BatchFormDialog from '../components/BatchFormDialog.vue';
import PageHeader from '../components/PageHeader.vue';

const store = useAquaStore();
const dialog = ref(false);
const stockDialog = ref(false);
const editedItem = ref<Pond | undefined>(undefined);
const stockPondId = ref('');

function openCreateDialog() {
  editedItem.value = undefined;
  dialog.value = true;
}

function openEditDialog(pond: Pond) {
  editedItem.value = pond;
  dialog.value = true;
}

function openStockDialog(pondId: string) {
    stockPondId.value = pondId;
    stockDialog.value = true;
}

function deletePond(id: string) {
  if (confirm('Are you sure you want to delete this pond?')) {
    store.deletePond(id);
  }
}

function savePond(pondData: any) {
  if (editedItem.value) {
    // Edit mode
    store.updatePond(editedItem.value.id, pondData);
  } else {
    // Create mode
    store.addPond(pondData);
  }
}

function saveBatch(batchData: Omit<Batch, 'id'>) {
    store.addBatch(batchData);
}

function handleAddSeed(payload: { batchId: string, seed: FishSeed }) {
    store.addFishSeed(payload.batchId, payload.seed);
}
</script>
