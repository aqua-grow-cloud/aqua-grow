<template>
  <v-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" max-width="600px">
    <v-card>
      <v-card-title>
        <span class="text-h5">Add Fish Seed to Batch</span>
      </v-card-title>

      <v-card-text>
        <v-container>
          <v-row>
            <v-col cols="12">
              <v-select
                v-model="form.batchId"
                :items="activeBatchOptions"
                item-title="title"
                item-value="value"
                label="Select Batch *"
                variant="outlined"
                density="compact"
                :rules="[v => !!v || 'Batch is required']"
              ></v-select>
            </v-col>

            <v-col cols="12">
              <v-select
                v-model="form.pondId"
                :items="pondOptions"
                item-title="name"
                item-value="id"
                label="Select Pond *"
                variant="outlined"
                density="compact"
                :rules="[v => !!v || 'Pond is required']"
              ></v-select>
            </v-col>

            <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.date"
                label="Seed Date *"
                type="date"
                variant="outlined"
                density="compact"
                :rules="[v => !!v || 'Date is required']"
              ></v-text-field>
            </v-col>

            <v-col cols="12" sm="6">
              <v-select
                v-model="form.fishTypeId"
                :items="fishTypes || []"
                item-title="name"
                item-value="id"
                label="Species/Type *"
                variant="outlined"
                density="compact"
                :rules="[v => !!v || 'Species is required']"
                placeholder="Select Species"
              ></v-select>
            </v-col>

            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.quantity"
                label="Quantity *"
                type="number"
                variant="outlined"
                density="compact"
                :rules="[v => v > 0 || 'Quantity must be greater than 0']"
              ></v-text-field>
            </v-col>

            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.sizeInches"
                label="Size (inches)"
                type="number"
                step="0.1"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>

            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.sizeGrams"
                label="Size (grams)"
                type="number"
                step="0.1"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>

            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.unitPrice"
                label="Unit Price"
                type="number"
                step="0.01"
                variant="outlined"
                density="compact"
                prefix="$"
              ></v-text-field>
            </v-col>

            <v-col cols="12">
              <v-select
                v-model="form.vendor"
                :items="supplierOptions"
                item-title="name"
                item-value="id"
                label="Vendor / Supplier"
                variant="outlined"
                density="compact"
                clearable
              ></v-select>
            </v-col>
          </v-row>
        </v-container>
      </v-card-text>

      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="grey" variant="text" @click="$emit('update:modelValue', false)">
          Cancel
        </v-btn>
        <v-btn color="primary" variant="elevated" @click="save" :disabled="!isValid">
          Add Seed
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type { Batch, Pond, Supplier, FishSeed } from '../../../api/generated/model';
import { fishTypeService } from '../../settings/api/fishTypeService';

const props = defineProps<{
  modelValue: boolean;
  batches: Batch[];
  ponds: Pond[];
  suppliers?: Supplier[];
}>();

const { data: fishTypes } = fishTypeService.useGetAll();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  'save': [payload: { batchId: string; seed: FishSeed }];
}>();

const defaultForm = {
  batchId: '',
  pondId: '',
  date: new Date().toISOString().substr(0, 10),
  fishTypeId: '',
  quantity: 0,
  sizeInches: undefined as number | undefined,
  sizeGrams: undefined as number | undefined,
  unitPrice: undefined as number | undefined,
  vendor: ''
};

const form = ref({ ...defaultForm });

const activeBatchOptions = computed(() => {
  return props.batches
    .filter(b => b.status === 'Active')
    .map(b => ({
      title: b.name || `Batch ${b.stockingDate}`,
      value: b.id
    }));
});

const pondOptions = computed(() => props.ponds || []);

const supplierOptions = computed(() => props.suppliers || []);

const isValid = computed(() => {
  return !!form.value.batchId && 
         !!form.value.pondId && 
         !!form.value.date && 
         !!form.value.fishTypeId && 
         form.value.quantity > 0;
});

function save() {
  if (!isValid.value) return;

  const seed: any = {
    pondId: form.value.pondId,
    date: form.value.date,
    fishTypeId: form.value.fishTypeId,
    quantity: form.value.quantity,
    sizeInches: form.value.sizeInches,
    sizeGrams: form.value.sizeGrams,
    unitPrice: form.value.unitPrice,
    vendor: form.value.vendor || undefined
  };

  emit('save', { batchId: form.value.batchId, seed });
  form.value = { ...defaultForm };
}
</script>
