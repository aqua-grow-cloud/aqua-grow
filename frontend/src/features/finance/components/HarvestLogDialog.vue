<template>
  <v-dialog v-model="show" max-width="600px">
    <v-card class="rounded-lg">
      <v-card-title class="bg-success text-white px-6 py-4">
        <v-icon icon="mdi-cash-edit" class="mr-2"></v-icon>
        Edit Incoming Payment
      </v-card-title>

      <v-card-text class="pt-6">
        <v-alert type="info" variant="tonal" density="compact" class="mb-4">
          Revenue, commission and weight adjustments are recalculated automatically from rate and biomass.
        </v-alert>

        <v-form ref="formRef" v-model="isFormValid" @submit.prevent="save">
          <v-row dense>
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

            <!-- Buyer -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.buyer"
                label="Buyer / Source"
                variant="outlined"
                density="compact"
                placeholder="e.g. Direct Sale"
              ></v-text-field>
            </v-col>

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
                clearable
              ></v-select>
            </v-col>

            <!-- Fish Type -->
            <v-col cols="12" sm="6">
              <v-select
                v-model="form.fishTypeId"
                :items="fishTypeItems"
                item-title="label"
                item-value="id"
                label="Fish Type"
                variant="outlined"
                density="compact"
                :rules="[v => !!v || 'Fish type is required']"
              ></v-select>
            </v-col>

            <!-- Biomass Weight -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.biomassWeight"
                label="Biomass Weight (kg)"
                type="number"
                variant="outlined"
                density="compact"
                suffix="kg"
                :rules="[v => (v !== null && v > 0) || 'Biomass must be > 0']"
              ></v-text-field>
            </v-col>

            <!-- Count -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.count"
                label="Count (pcs)"
                type="number"
                variant="outlined"
                density="compact"
                suffix="pcs"
              ></v-text-field>
            </v-col>

            <!-- Avg Weight -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.avgWeightGrams"
                label="Avg Weight (g)"
                type="number"
                variant="outlined"
                density="compact"
                suffix="g"
              ></v-text-field>
            </v-col>

            <!-- Basic Rate -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.basicRate"
                label="Basic Rate (₹/kg)"
                type="number"
                variant="outlined"
                density="compact"
                prefix="₹"
                :rules="[v => (v !== null && v > 0) || 'Rate must be > 0']"
              ></v-text-field>
            </v-col>

            <!-- Commission Rate -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.commission"
                label="Commission (₹)"
                type="number"
                variant="outlined"
                density="compact"
                prefix="₹"
                hint="Fixed commission amount"
                persistent-hint
              ></v-text-field>
            </v-col>

            <!-- Is Final -->
            <v-col cols="12" sm="6" class="d-flex align-center">
              <v-checkbox
                v-model="form.isFinal"
                label="Final Harvest"
                density="compact"
                hide-details
                color="warning"
              ></v-checkbox>
            </v-col>

            <!-- Read-only derived values -->
            <v-col cols="12">
              <v-divider class="my-2"></v-divider>
              <div class="text-caption text-grey mb-2">Derived values (recalculated on save)</div>
              <v-row dense>
                <v-col cols="6" sm="3">
                  <v-text-field
                    :model-value="formatCurrency(log?.discountWeight)"
                    label="Discount Weight"
                    variant="outlined"
                    density="compact"
                    suffix="kg"
                    readonly
                    bg-color="grey-lighten-4"
                  ></v-text-field>
                </v-col>
                <v-col cols="6" sm="3">
                  <v-text-field
                    :model-value="formatCurrency(log?.actualWeight)"
                    label="Actual Weight"
                    variant="outlined"
                    density="compact"
                    suffix="kg"
                    readonly
                    bg-color="grey-lighten-4"
                  ></v-text-field>
                </v-col>
                <v-col cols="6" sm="3">
                  <v-text-field
                    :model-value="formatCurrency(log?.finalRate)"
                    label="Final Rate (₹/kg)"
                    variant="outlined"
                    density="compact"
                    prefix="₹"
                    readonly
                    bg-color="grey-lighten-4"
                  ></v-text-field>
                </v-col>
                <v-col cols="6" sm="3">
                  <v-text-field
                    :model-value="formatCurrency(log?.revenue)"
                    label="Revenue"
                    variant="outlined"
                    density="compact"
                    prefix="₹"
                    readonly
                    bg-color="green-lighten-5"
                    class="font-weight-bold"
                  ></v-text-field>
                </v-col>
              </v-row>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>

      <v-card-actions class="px-6 pb-6">
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="close">Cancel</v-btn>
        <v-btn color="success" variant="flat" :disabled="!isFormValid" :loading="saving" @click="save">
          Update Payment
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {useBatches} from '../../batches/composables/useBatches';
import {fishTypeService} from '../../settings/api/fishTypeService';
import type {HarvestLog} from '@/api/generated/model';

const props = defineProps<{
  modelValue: boolean;
  log?: HarvestLog | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'update', data: HarvestLog): void;
}>();

const { batches } = useBatches();
const { data: fishTypes } = fishTypeService.useGetAll();

const formRef = ref<any>(null);
const isFormValid = ref(false);
const saving = ref(false);

const show = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const batchItems = computed(() =>
  batches.value?.map(b => ({ id: b.id, label: b.name || b.stockingDate || b.id })) || []
);

const fishTypeItems = computed(() =>
  fishTypes.value?.map(f => ({ id: f.id, label: f.name || f.id })) || []
);

const defaultForm = () => ({
  date: new Date().toISOString().slice(0, 10),
  buyer: '',
  batchId: '',
  fishTypeId: '',
  pondId: '',
  biomassWeight: 0,
  count: 0,
  avgWeightGrams: 0,
  basicRate: 0,
  commission: 0,
  isFinal: false,
});

const form = ref(defaultForm());

watch(
  () => props.modelValue,
  (open) => {
    if (open && props.log) {
      form.value = {
        date: (props.log.date as string) || new Date().toISOString().slice(0, 10),
        buyer: props.log.buyer || '',
        batchId: props.log.batchId || '',
        fishTypeId: props.log.fishTypeId || '',
        pondId: props.log.pondId || '',
        biomassWeight: props.log.biomassWeight ?? 0,
        count: props.log.count ?? 0,
        avgWeightGrams: props.log.avgWeightGrams ?? 0,
        basicRate: props.log.basicRate ?? 0,
        commission: props.log.commission ?? 0,
        isFinal: props.log.isFinal ?? false,
      };
    }
  }
);

function close() {
  show.value = false;
}

function formatCurrency(val?: number | null): string {
  if (val === undefined || val === null) return '—';
  return val.toLocaleString('en-IN', { minimumFractionDigits: 2 });
}

function save() {
  if (!isFormValid.value || !props.log?.id) return;

  const payload: HarvestLog = {
    ...props.log,
    date: form.value.date as any,
    buyer: form.value.buyer || undefined,
    batchId: form.value.batchId || undefined,
    fishTypeId: form.value.fishTypeId,
    pondId: form.value.pondId || undefined,
    biomassWeight: form.value.biomassWeight,
    count: form.value.count,
    avgWeightGrams: form.value.avgWeightGrams,
    basicRate: form.value.basicRate,
    commission: form.value.commission,
    isFinal: form.value.isFinal,
  };

  emit('update', payload);
  close();
}
</script>
