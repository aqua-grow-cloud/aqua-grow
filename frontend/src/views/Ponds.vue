<template>
  <v-container fluid>
    <PageHeader
      title="Ponds"
      :action-label="store.isManager ? 'Add Pond' : undefined"
      @action="openCreateDialog"
    />

    <div v-if="isLoading" class="d-flex justify-center align-center py-12">
      <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
    </div>

    <template v-else>
      <v-row>
        <v-col v-for="pond in ponds" :key="pond.id" cols="12" sm="6" md="4" lg="3">
          <PondCard
            :pond="pond"
            :can-manage="store.isManager"
            @edit="openEditDialog"
            @delete="removePond"
            @stock="openStockDialog"
          />
        </v-col>
      </v-row>

      <!-- Empty State -->
      <v-alert
        v-if="!ponds || ponds.length === 0"
        type="info"
        variant="tonal"
        class="mt-4"
      >
        No ponds found. Create one to get started.
      </v-alert>
    </template>

    <PondFormDialog
      v-model="dialog"
      :item="editedItem"
      @save="savePond"
    />

    <BatchFormDialog
        v-model="stockDialog"
        :initial-pond-id="stockPondId"
        :ponds="ponds || []"
        :batches="batches || []"
        @save="saveBatch"
        @add-seed="handleAddSeed"
    />
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import { usePonds } from '../composables/usePonds';
import { useBatches } from '../composables/useBatches';
import type { FishSeed } from '../api/manual/types';
import type { Pond } from '../api/generated/model';
import PondCard from '../components/PondCard.vue';
import PondFormDialog from '../components/PondFormDialog.vue';
import BatchFormDialog from '../components/BatchFormDialog.vue';
import PageHeader from '../components/PageHeader.vue';

const store = useAquaStore();

// Use composables
const { ponds, isLoading, create: createPond, update: updatePond, remove: deletePond } = usePonds();
const { batches, create: createBatch } = useBatches();

const dialog = ref(false);
const stockDialog = ref(false);
const editedItem = ref<Pond | undefined>(undefined);
const stockPondId = ref<string | undefined>(undefined);

function openCreateDialog() {
  editedItem.value = undefined;
  dialog.value = true;
}

function openEditDialog(pond: Pond) {
  editedItem.value = pond;
  dialog.value = true;
}

function openStockDialog(pondId?: string) {
    stockPondId.value = pondId;
    stockDialog.value = true;
}

async function removePond(id?: string) {
  if (!id) return;
  if (confirm('Are you sure you want to delete this pond?')) {
    await deletePond(id);
  }
}

async function savePond(pondData: any) {
  if (editedItem.value?.id) {
    // Edit mode
    await updatePond(editedItem.value.id, pondData);
  } else {
    // Create mode
    await createPond(pondData);
  }
  dialog.value = false;
}

async function saveBatch(batchData: any) {
    await createBatch(batchData);
    stockDialog.value = false;
}

function handleAddSeed(payload: { batchId: string, seed: FishSeed }) {
    // TODO: Implement with real API if needed, or handle within Batch creation
    store.addFishSeed(payload.batchId, payload.seed as any); // Cast to any for store compatibility for now
}
</script>
