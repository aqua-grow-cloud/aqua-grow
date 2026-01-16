<template>
  <v-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" max-width="600px">
    <v-card>
      <v-card-title>
        <span class="text-h5">{{ modeLabel }}</span>
      </v-card-title>

      <v-card-text>
        <v-container>
           <!-- Alert for Existing Batch Context -->
           <v-alert v-if="existingBatch" type="info" variant="tonal" class="mb-4" density="compact">
             Selected: <strong>{{ existingBatch.name || 'Unnamed Batch' }}</strong> in {{ props.ponds.find(p => p.id === form.pondId)?.name }}.
             <br>
             Stock: {{ [...new Set(existingBatch?.fishSeeds?.map(s => getFishTypeName(s.fishTypeId)) || [])].join(', ') }} ({{ existingBatch?.fishSeeds?.reduce((acc, s) => acc + (s.quantity ?? 0), 0) ?? 0 }})
           </v-alert>

          <v-alert v-if="availablePonds.length === 0" type="warning" class="mb-4">
            No active ponds available. Please activate a pond first.
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

            <!-- Target Batch Selection (Only if pond has active batches) -->
            <v-col cols="12" sm="6" v-if="pondActiveBatches.length > 0">
                 <v-select
                    v-model="targetBatchId"
                    :items="batchOptions"
                    item-title="title"
                    item-value="value"
                    label="Action"
                    variant="outlined"
                    density="compact"
                    hide-details
                 ></v-select>
            </v-col>

            <!-- Batch Name (Only for New Batch) -->
            <v-col cols="12" v-if="isNewBatchMode">
              <v-text-field
                v-model="form.name"
                label="Batch Name (e.g. Batch1)"
                variant="outlined"
                density="compact"
                hide-details
              ></v-text-field>
            </v-col>

            <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.stockingDate"
                :label="isNewBatchMode ? 'Stocking Date' : 'Seed Date'"
                type="date"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6" v-if="isNewBatchMode">
              <v-text-field
                v-model="form.estimatedHarvestDate"
                label="Est. Harvest Date"
                type="date"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-select
                v-model="form.fishTypeId"
                :items="fishTypes || []"
                item-title="name"
                item-value="id"
                :label="isNewBatchMode ? 'Species' : 'Type (Species)'"
                variant="outlined"
                density="compact"
                placeholder="Select Species"
                required
              ></v-select>
            </v-col>
             <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.quantity"
                :label="isNewBatchMode ? 'Initial Quantity' : 'Quantity'"
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
            <v-col cols="12">
              <v-select
                v-model="form.vendor"
                :items="suppliers || []"
                item-title="name"
                item-value="name"
                label="Vendor / Supplier"
                variant="outlined"
                density="compact"
                clearable
                placeholder="Select supplier..."
              ></v-select>
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
import type { Batch, FishSeed, Pond } from '../../../api/generated/model';
import { supplierService } from '../../inventory/api/supplierService';
import { fishTypeService } from '../../settings/api/fishTypeService';

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

const { data: suppliers } = supplierService.useGetAll();
const { data: fishTypes } = fishTypeService.useGetAll();

// Filter ponds that don't have an active batch and are active themselves
// Show all active ponds, so we can add seeds to existing batches too
const availablePonds = computed(() => {
  return props.ponds.filter(p => p.status === 'Active');
});

const defaultForm = {
  name: '',
  pondId: '',
  fishTypeId: '',
  stockingDate: new Date().toISOString().substr(0, 10),
  estimatedHarvestDate: '',
  quantity: 0,
  sizeInches: null as number | null,
  sizeGrams: null as number | null,
  vendor: '',
};

const form = ref({ ...defaultForm });
const targetBatchId = ref<string>('NEW');

// Existing active batches for the selected pond
const pondActiveBatches = computed(() => {
    if (!form.value.pondId) return [];
    return props.batches.filter(b => b.status === 'Active' && b.fishSeeds?.some(s => s.pondId === form.value.pondId && s.status === 'Active'));
});

// Options for the Action dropdown
const batchOptions = computed(() => {
    const options = [
        { title: 'Start New Batch', value: 'NEW' }
    ];
    pondActiveBatches.value.forEach(b => {
        options.push({ title: `Add to: ${b.name || 'Unnamed'}`, value: b.id || '' });
    });
    return options;
});

// Determine if we are in New Batch Mode
const isNewBatchMode = computed(() => targetBatchId.value === 'NEW');

// Get the existing batch object if not in new mode
const existingBatch = computed(() => {
    if (isNewBatchMode.value) return null;
    return pondActiveBatches.value.find(b => b.id === targetBatchId.value) || null;
});

const modeLabel = computed(() => isNewBatchMode.value ? 'Start New Batch' : 'Add Fish Seed');
const btnLabel = computed(() => isNewBatchMode.value ? 'Start Batch' : 'Add Seed');

// Watch pond selection to reset or set default target logic
watch(() => form.value.pondId, (newPondId) => {
    // If there are existing batches, default to the first one for convenience (old behavior),
    // OR default to NEW? Let's default to the *most recent* active batch to be helpful,
    // but if the user wants NEW, they can select it.
    // Given the request "create a second new batch", defaulting to NEW might be annoying if usually you add seeds.
    // Let's default to first active batch if exists, else NEW.
    const batches = props.batches.filter(b => b.status === 'Active' && b.fishSeeds?.some(s => s.pondId === newPondId && s.status === 'Active'));
    if (batches.length > 0) {
        targetBatchId.value = batches[0]?.id || 'NEW';
    } else {
        targetBatchId.value = 'NEW';
    }
});

watch(() => props.modelValue, (val) => {
    if (val) {
        if (props.initialPondId) {
            form.value.pondId = props.initialPondId;
        }
    } else {
        form.value = { ...defaultForm };
        targetBatchId.value = 'NEW';
    }
});

const isValid = computed(() => {
  return form.value.pondId && form.value.fishTypeId && form.value.quantity > 0;
});

function close() {
  emit('update:modelValue', false);
  form.value = { ...defaultForm };
  targetBatchId.value = 'NEW';
}

function getFishTypeName(id?: string) {
    if (!id) return '-';
    return (fishTypes.value as any)?.find((t: any) => t.id === id)?.name || id;
}

function save() {
  const seed: any = {
      id: undefined,
      pondId: form.value.pondId,
      date: form.value.stockingDate,
      fishTypeId: form.value.fishTypeId,
      quantity: form.value.quantity,
      sizeInches: form.value.sizeInches || undefined,
      sizeGrams: form.value.sizeGrams || undefined,
      vendor: form.value.vendor || undefined
  };

  if (!isNewBatchMode.value && existingBatch.value && existingBatch.value.id) {
      // Add to existing
      emit('add-seed', { batchId: existingBatch.value.id, seed });
  } else {
      // New Batch
      const batchData: any = {
          name: form.value.name,
          stockingDate: form.value.stockingDate,
          estimatedHarvestDate: form.value.estimatedHarvestDate || undefined,
          status: 'Active',
          fishSeeds: [{
              pondId: form.value.pondId,
              date: form.value.stockingDate,
              fishTypeId: form.value.fishTypeId,
              quantity: form.value.quantity,
              sizeInches: form.value.sizeInches || undefined,
              sizeGrams: form.value.sizeGrams || undefined,
              vendor: form.value.vendor || undefined
          }]
      };
      emit('save', batchData);
  }
  close();
}
</script>
