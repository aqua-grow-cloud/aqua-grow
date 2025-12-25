<template>
  <v-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" max-width="600px">
    <v-card>
      <v-card-title>
        <span class="text-h5">{{ modeLabel }}</span>
      </v-card-title>

      <v-card-text>
        <v-container>
           <v-alert v-if="existingBatch" type="info" variant="tonal" class="mb-4" density="compact">
             Adding to active batch in <strong>{{ props.ponds.find(p => p.id === form.pondId)?.name }}</strong>.
             Current stock: {{ existingBatch.species }} ({{ existingBatch.initialQuantity }})
           </v-alert>

          <v-alert v-if="availablePonds.length === 0" type="warning" class="mb-4">
            No active ponds available. Please activate a pond first.
          </v-alert>
          
          <v-row v-else>
            <v-col cols="12">
              <v-text-field
                v-model="form.name"
                label="Batch Name (e.g. Batch1)"
                variant="outlined"
                density="compact"
                hide-details
              ></v-text-field>
            </v-col>
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
                v-model="form.estimatedHarvestDate"
                label="Est. Harvest Date"
                type="date"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.type"
                label="Type (Species)"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
             <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.quantity"
                label="Quantity"
                type="number"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.sizeInches"
                label="Size (inches)"
                type="number"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.sizeGrams"
                label="Size (grams)"
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
          {{ btnLabel }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { Batch, FishSeed, Pond } from '../api/manual/types';

interface Props {
  modelValue: boolean;
  initialPondId?: string;
  ponds: Pond[];
  batches: Batch[];
}

const props = withDefaults(defineProps<Props>(), {
    ponds: () => [],
    batches: () => []
});

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'save', batch: Omit<Batch, 'id'>): void;
  (e: 'add-seed', payload: { batchId: string; seed: FishSeed }): void;
}>();

// Filter ponds that don't have an active batch and are active themselves
// Show all active ponds, so we can add seeds to existing batches too
const availablePonds = computed(() => {
  return props.ponds.filter(p => p.status === 'Active');
});

const defaultForm = {
  name: '',
  pondId: '',
  type: 'Catla',
  stockingDate: new Date().toISOString().substr(0, 10),
  estimatedHarvestDate: '',
  quantity: 0,
  sizeInches: null as number | null,
  sizeGrams: null as number | null,
};

const form = ref({ ...defaultForm });

// Check if the selected pond has an active batch
const existingBatch = computed(() => {
    if (!form.value.pondId) return null;
    // Find batch where pondId matches and status is Active
    // We can rely on batches prop instead of pond.currentBatchId link from store
    return props.batches.find(b => b.pondId === form.value.pondId && b.status === 'Active') || null;
});

const modeLabel = computed(() => existingBatch.value ? 'Add Fish Seed' : 'Start New Batch');
const btnLabel = computed(() => existingBatch.value ? 'Add Seed' : 'Start Batch');

watch(() => props.modelValue, (val) => {
    if (val && props.initialPondId) {
        form.value.pondId = props.initialPondId;
    }
});

const isValid = computed(() => {
  return form.value.pondId && form.value.type && form.value.quantity > 0;
});

function close() {
  emit('update:modelValue', false);
  form.value = { ...defaultForm };
}

function save() {
  const seed: FishSeed = {
      // Don't generate ID here. Backend will auto-generate one.
      // If we send an ID that doesn't exist, backend throws detached entity exception.
      id: undefined, 
      batchId: existingBatch.value ? existingBatch.value.id : '', 
      date: form.value.stockingDate,
      type: form.value.type,
      quantity: form.value.quantity,
      sizeInches: form.value.sizeInches || undefined,
      sizeGrams: form.value.sizeGrams || undefined
  };

  if (existingBatch.value && existingBatch.value.id) {
      // Add to existing
      emit('add-seed', { batchId: existingBatch.value.id, seed });
  } else {
      // New Batch
      const batchData: Omit<Batch, 'id'> = {
          pondId: form.value.pondId,
          name: form.value.name,
          species: form.value.type,
          stockingDate: form.value.stockingDate,
          estimatedHarvestDate: form.value.estimatedHarvestDate || undefined,
          initialQuantity: form.value.quantity,
          seedSize: form.value.sizeGrams || 0,
          status: 'Active',
          fishSeeds: [] // Separated creation: Batch starts empty of specific seed records
      };
      emit('save', batchData);
  }
  close();
}
</script>
