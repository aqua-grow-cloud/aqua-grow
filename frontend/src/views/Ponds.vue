<template>
  <v-container fluid>
    <PageHeader 
      title="Ponds" 
      action-label="Add Pond" 
      @action="openCreateDialog" 
    />

    <v-row>
      <v-col v-for="pond in store.ponds" :key="pond.id" cols="12" sm="6" md="4" lg="3">
        <PondCard 
          :pond="pond" 
          @edit="openEditDialog" 
          @delete="deletePond"
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
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import type { Pond } from '../types';
import PondCard from '../components/PondCard.vue';
import PondFormDialog from '../components/PondFormDialog.vue';
import PageHeader from '../components/PageHeader.vue';

const store = useAquaStore();
const dialog = ref(false);
const editedItem = ref<Pond | undefined>(undefined);

function openCreateDialog() {
  editedItem.value = undefined;
  dialog.value = true;
}

function openEditDialog(pond: Pond) {
  editedItem.value = pond;
  dialog.value = true;
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
</script>
