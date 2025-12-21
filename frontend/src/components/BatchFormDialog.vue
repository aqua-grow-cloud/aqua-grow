<template>
  <v-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" max-width="600px">
    <v-card>
      <v-card-title>
        <span class="text-h5">New Batch</span>
      </v-card-title>

      <v-card-text>
        <v-container>
          <v-alert v-if="availablePonds.length === 0" type="warning" class="mb-4">
            No active ponds available from which to create a batch. Please activate a pond first.
          </v-alert>

          <v-row v-else>
            <v-col cols="12" sm="6">
              <v-select
                v-model="form.pondId"
                :items="availablePonds"
                item-title="name"
                item-value="id"
                label="Select Pond"
                variant="outlined"
                density="compact"
                :rules="[v => !!v || 'Pond is required']"
              ></v-select>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.stockingDate"
                label="Stocking Date"
                type="date"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.species"
                label="Species"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
             <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.strain"
                label="Strain (Optional)"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.initialQuantity"
                label="Quantity (pieces)"
                type="number"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.seedSize"
                label="Seed Size (g or PL)"
                type="number"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
          </v-row>
        </v-container>
      </v-card-text>

      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="blue-darken-1" variant="text" @click="close">
          Cancel
        </v-btn>
        <v-btn color="blue-darken-1" variant="text" @click="save" :disabled="!isValid">
          Start Batch
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAquaStore } from '../stores/useAquaStore';
import type { Batch } from '../types';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'save', batch: Omit<Batch, 'id'>): void;
}>();

const store = useAquaStore();

// Filter ponds that don't have an active batch and are active themselves
const availablePonds = computed(() => {
  return store.ponds.filter(p => p.status === 'Active' && !p.currentBatchId);
});

const defaultForm = {
  pondId: '',
  species: 'L. vannamei',
  strain: '',
  stockingDate: new Date().toISOString().substr(0, 10),
  initialQuantity: 0,
  seedSize: 0,
  status: 'Active' as const,
};

const form = ref({ ...defaultForm });

const isValid = computed(() => {
  return form.value.pondId && form.value.species && form.value.initialQuantity > 0;
});

function close() {
  emit('update:modelValue', false);
  form.value = { ...defaultForm };
}

function save() {
  emit('save', { ...form.value });
  close();
}
</script>
