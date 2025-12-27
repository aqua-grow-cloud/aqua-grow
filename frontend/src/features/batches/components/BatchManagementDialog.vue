<template>
  <v-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" max-width="500px">
    <v-card>
      <v-card-title>
        <span class="text-h5">{{ isEditMode ? 'Edit Batch' : 'New Batch' }}</span>
      </v-card-title>

      <v-card-text>
        <v-container>
          <v-row>
            <v-col cols="12">
              <v-text-field
                v-model="form.name"
                label="Batch Name *"
                variant="outlined"
                density="compact"
                :rules="[v => !!v || 'Batch name is required']"
                placeholder="e.g., Batch A, Spring 2025"
              ></v-text-field>
            </v-col>

            <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.stockingDate"
                label="Stocking Date *"
                type="date"
                variant="outlined"
                density="compact"
                :rules="[v => !!v || 'Stocking date is required']"
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

            <v-col cols="12">
              <v-select
                v-model="form.status"
                :items="statusOptions"
                label="Status"
                variant="outlined"
                density="compact"
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
          {{ isEditMode ? 'Update' : 'Create' }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { Batch } from '../../../api/generated/model';

const props = defineProps<{
  modelValue: boolean;
  batch?: Batch;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  'save': [batch: Partial<Batch>];
}>();

const statusOptions = ['Active', 'Completed'];

const defaultForm = {
  name: '',
  stockingDate: new Date().toISOString().substr(0, 10),
  estimatedHarvestDate: '',
  status: 'Active'
};

const form = ref({ ...defaultForm });

const isEditMode = computed(() => !!props.batch?.id);

const isValid = computed(() => {
  return !!form.value.name && !!form.value.stockingDate;
});

watch(() => props.batch, (newBatch) => {
  if (newBatch) {
    form.value = {
      name: newBatch.name || '',
      stockingDate: newBatch.stockingDate || '',
      estimatedHarvestDate: newBatch.estimatedHarvestDate || '',
      status: newBatch.status || 'Active'
    };
  } else {
    form.value = { ...defaultForm };
  }
}, { immediate: true });

function save() {
  if (!isValid.value) return;

  const batchData: Partial<Batch> = {
    ...form.value,
    fishSeeds: props.batch?.fishSeeds || []
  };

  if (isEditMode.value) {
    batchData.id = props.batch!.id;
  }

  emit('save', batchData);
  form.value = { ...defaultForm };
}
</script>
