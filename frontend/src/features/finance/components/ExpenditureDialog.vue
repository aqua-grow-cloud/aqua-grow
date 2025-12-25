<template>
  <v-dialog v-model="show" max-width="500px">
    <v-card class="rounded-lg">
      <v-card-title class="bg-primary text-white px-6 py-4">
        Log Manual Expenditure
      </v-card-title>
      <v-card-text class="pt-6">
        <v-form ref="formRef" v-model="isFormValid" @submit.prevent="save">
          <v-row dense>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.date"
                label="Date"
                type="date"
                variant="outlined"
                density="compact"
                required
                :rules="[v => !!v || 'Date is required']"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-select
                v-model="form.batchId"
                :items="activeBatches"
                item-title="label"
                item-value="id"
                label="Batch (Optional)"
                variant="outlined"
                density="compact"
                clearable
              ></v-select>
            </v-col>
            <v-col cols="12" sm="6">
              <v-select
                v-model="form.macroInvestment"
                :items="['ADVANCE', 'LEASE', 'INVESTMENT', 'NO_RETURN', 'MAINTENANCE', 'OTHER']"
                label="Macro Investment"
                variant="outlined"
                density="compact"
                required
                :rules="[v => !!v || 'Macro Investment is required']"
              ></v-select>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model="form.subType"
                label="Type / Category"
                variant="outlined"
                density="compact"
                placeholder="e.g. Annual Lease"
              ></v-text-field>
            </v-col>
            <v-col cols="12">
              <v-text-field
                v-model.number="form.amount"
                label="Amount (₹)"
                type="number"
                variant="outlined"
                density="compact"
                required
                prefix="₹"
                :rules="[v => !!v || 'Amount is required', v => v > 0 || 'Amount must be greater than 0']"
              ></v-text-field>
            </v-col>
            <v-col cols="12">
              <v-textarea
                v-model="form.description"
                label="Description"
                variant="outlined"
                density="compact"
                rows="2"
              ></v-textarea>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions class="px-6 pb-6">
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="close">Cancel</v-btn>
        <v-btn color="primary" variant="flat" :disabled="!isFormValid" @click="save">Save Entry</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useAquaStore } from '../../../stores/useAquaStore';
import { useBatches } from '../../batches/composables/useBatches';
import type { Expenditure, Batch } from '../../../api/generated/model';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'save', data: Omit<Expenditure, 'id'>): void;
}>();

const store = useAquaStore();
const { batches } = useBatches();
const formRef = ref<any>(null);
const isFormValid = ref(false);

const activeBatches = computed(() => {
  return batches.value?.filter((b: Batch) => b.status === 'Active').map((b: Batch) => ({
    id: b.id,
    label: b.name || b.stockingDate
  })) || [];
});

const defaultForm = {
  date: new Date().toISOString().substr(0, 10),
  batchId: '',
  macroInvestment: 'INVESTMENT',
  subType: '',
  amount: 0,
  description: '',
  category: 'Other' as any
};

const form = ref({ ...defaultForm });

const show = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

watch(() => props.modelValue, (val) => {
  if (val) {
    form.value = { ...defaultForm };
  }
});

function close() {
  show.value = false;
}

function save() {
  if (!isFormValid.value) return;

  // Map macroInvestment to category for compatibility with existing model
  let category = 'Other' as Expenditure['category'];
  if (form.value.macroInvestment === 'LEASE') category = 'Lease';
  if (form.value.macroInvestment === 'ADVANCE') category = 'Advance';
  if (form.value.macroInvestment === 'NO_RETURN') category = 'NoReturn';
  if (form.value.macroInvestment === 'MAINTENANCE') category = 'Maintenance';

  const payload: Omit<Expenditure, 'id'> = {
    date: form.value.date,
    category: category,
    macroInvestment: form.value.macroInvestment,
    subType: form.value.subType,
    batchId: form.value.batchId || undefined,
    amount: form.value.amount,
    description: form.value.description
  };

  emit('save', payload);
  close();
}
</script>
