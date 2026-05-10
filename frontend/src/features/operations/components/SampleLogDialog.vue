<template>
  <v-dialog v-model="show" max-width="520px">
    <v-card class="rounded-lg">
      <v-card-title class="bg-primary text-white px-6 py-4">
        <v-icon icon="mdi-flask-edit" class="mr-2"></v-icon>
        Edit Sample Log
      </v-card-title>

      <v-card-text class="pt-6">
        <v-form ref="formRef" v-model="isFormValid" @submit.prevent="save">
          <v-row dense>
            <!-- Batch -->
            <v-col cols="12" sm="6">
              <v-select
                v-model="form.batchId"
                :items="batchItems"
                item-title="label"
                item-value="id"
                label="Batch"
                variant="outlined"
                density="compact"
                :rules="[v => !!v || 'Batch is required']"
              ></v-select>
            </v-col>

            <!-- Fish Type -->
            <v-col cols="12" sm="6">
              <v-select
                v-model="form.fishTypeId"
                :items="availableFishTypes"
                item-title="name"
                item-value="id"
                label="Fish Type / Species"
                variant="outlined"
                density="compact"
                :disabled="!form.batchId"
                :rules="[v => !!v || 'Fish type is required']"
              ></v-select>
            </v-col>

            <!-- Date -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.date"
                label="Date"
                type="date"
                variant="outlined"
                density="compact"
                :rules="[v => !!v || 'Date is required']"
              ></v-text-field>
            </v-col>

            <!-- Sample Count -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.sampleCount"
                label="Pieces (Count)"
                type="number"
                variant="outlined"
                density="compact"
                :rules="[v => (v > 0) || 'Must be > 0']"
              ></v-text-field>
            </v-col>

            <!-- Total Weight -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.totalSampleWeight"
                label="Total Sample Weight (g)"
                type="number"
                variant="outlined"
                density="compact"
                suffix="g"
                :rules="[v => (v > 0) || 'Must be > 0']"
              ></v-text-field>
            </v-col>

            <!-- Avg Weight (auto-calculated) -->
            <v-col cols="12" sm="6">
              <v-text-field
                :model-value="calculatedABW"
                label="Avg Weight / ABW (g)"
                type="number"
                variant="outlined"
                density="compact"
                suffix="g"
                readonly
                bg-color="grey-lighten-4"
                hint="Auto-calculated from count & total weight"
                persistent-hint
              ></v-text-field>
            </v-col>

            <!-- Avg Length -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.averageLength"
                label="Avg Length (cm)"
                type="number"
                variant="outlined"
                density="compact"
                suffix="cm"
              ></v-text-field>
            </v-col>

            <!-- Survival Estimate -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.survivalEstimate"
                label="Survival Estimate (%)"
                type="number"
                variant="outlined"
                density="compact"
                suffix="%"
                :rules="[v => (v === null || v === undefined || (v >= 0 && v <= 100)) || 'Must be 0–100']"
              ></v-text-field>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>

      <v-card-actions class="px-6 pb-6">
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="close">Cancel</v-btn>
        <v-btn color="primary" variant="flat" :disabled="!isFormValid" @click="save">
          Update Sample
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {useBatches} from '../../batches/composables/useBatches';
import {fishTypeService} from '../../settings/api/fishTypeService';
import type {SampleLog} from '@/api/generated/model';

const props = defineProps<{
  modelValue: boolean;
  log?: SampleLog | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'update', data: SampleLog): void;
}>();

const { batches, getBatchById } = useBatches();
const { data: fishTypes } = fishTypeService.useGetAll();

const formRef = ref<any>(null);
const isFormValid = ref(false);

const show = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const batchItems = computed(() =>
  batches.value?.map(b => ({ id: b.id, label: b.name || b.stockingDate || b.id })) || []
);

// Fish types scoped to selected batch's seeds
const availableFishTypes = computed(() => {
  if (!form.value.batchId) return fishTypes.value || [];
  const batch = getBatchById(form.value.batchId);
  if (!batch?.fishSeeds) return fishTypes.value || [];
  const ids = new Set(batch.fishSeeds.map((s: any) => s.fishTypeId));
  return (fishTypes.value || []).filter((ft: any) => ids.has(ft.id));
});

const defaultForm = (): SampleLog => ({
  batchId: '',
  fishTypeId: '',
  date: new Date().toISOString().slice(0, 10),
  sampleCount: 0,
  totalSampleWeight: 0,
  averageWeight: 0,
  averageLength: 0,
  survivalEstimate: 100,
});

const form = ref<SampleLog>(defaultForm());

const calculatedABW = computed(() => {
  const count = form.value.sampleCount ?? 0;
  const weight = form.value.totalSampleWeight ?? 0;
  if (count > 0 && weight > 0) {
    return parseFloat((weight / count).toFixed(2));
  }
  return 0;
});

watch(calculatedABW, (val) => {
  form.value.averageWeight = val;
});

// Reset fishTypeId when batch changes
watch(() => form.value.batchId, () => {
  form.value.fishTypeId = '';
});

watch(
  () => props.modelValue,
  (open) => {
    if (open && props.log) {
      form.value = {
        ...props.log,
        date: (props.log.date as string) || new Date().toISOString().slice(0, 10),
      };
    } else if (open) {
      form.value = defaultForm();
    }
  }
);

function close() {
  show.value = false;
}

function save() {
  if (!isFormValid.value || !props.log?.id) return;
  emit('update', { ...form.value, averageWeight: calculatedABW.value, id: props.log.id });
  close();
}
</script>
