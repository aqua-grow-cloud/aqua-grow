<template>
  <v-dialog v-model="show" max-width="500px">
    <v-card class="rounded-lg">
      <v-card-title class="bg-primary text-white px-6 py-4">
        {{ isEditMode ? 'Edit Expenditure' : 'Log Manual Expenditure' }}
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
              <v-combobox
                v-model="form.macroInvestment"
                :items="macroInvestmentOptions"
                label="Macro Investment"
                variant="outlined"
                density="compact"
                required
                :rules="[v => !!v || 'Macro Investment is required']"
                hint="Pick an existing value or type a new one"
                persistent-hint
              ></v-combobox>
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
                hint="Use negative values for corrections or refunds"
                persistent-hint
                :rules="[v => v !== null && v !== '' && v !== undefined || 'Amount is required']"
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
        <v-btn color="primary" variant="flat" :disabled="!isFormValid" @click="save">
          {{ isEditMode ? 'Update Entry' : 'Save Entry' }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue';
import {useBatches} from '../../batches/composables/useBatches';
import {expenditureService} from '../api/expenditureService';
import type {Batch, Expenditure} from '@/api/generated/model';

const props = defineProps<{
  modelValue: boolean;
  expenditure?: Expenditure | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'save', data: Omit<Expenditure, 'id'>): void;
  (e: 'update', data: Expenditure): void;
}>();

const { batches } = useBatches();
const formRef = ref<any>(null);
const isFormValid = ref(false);
const macroInvestmentOptions = ref<string[]>([]);

onMounted(async () => {
  try {
    macroInvestmentOptions.value = await expenditureService.getMacroInvestments();
  } catch {
    // fallback to empty — user can still type freely
  }
});

const isEditMode = computed(() => !!props.expenditure?.id);

const activeBatches = computed(() => {
  return batches.value?.filter((b: Batch) => b.status === 'Active').map((b: Batch) => ({
    id: b.id,
    label: b.name || b.stockingDate
  })) || [];
});

const latestActiveBatchId = computed(() => {
  const active = batches.value?.filter((b: Batch) => b.status === 'Active') || [];
  if (active.length === 0) return '';
  const sorted = active.length === 1
    ? active
    : [...active].sort((a, b) => (b.stockingDate || '').localeCompare(a.stockingDate || ''));
  return sorted.find(() => true)?.id ?? '';
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
    if (props.expenditure) {
      // Pre-fill form for edit mode
      form.value = {
        date: props.expenditure.date || defaultForm.date,
        batchId: props.expenditure.batchId || '',
        macroInvestment: props.expenditure.macroInvestment || 'INVESTMENT',
        subType: props.expenditure.subType || '',
        amount: props.expenditure.amount ?? 0,
        description: props.expenditure.description || '',
        category: props.expenditure.category || 'Other'
      };
    } else {
      form.value = { ...defaultForm, batchId: latestActiveBatchId.value };
    }
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

  if (isEditMode.value && props.expenditure?.id) {
    const payload: Expenditure = {
      id: props.expenditure.id,
      date: form.value.date,
      category: category,
      macroInvestment: form.value.macroInvestment,
      subType: form.value.subType,
      batchId: form.value.batchId || undefined,
      amount: form.value.amount,
      description: form.value.description,
      tenantId: props.expenditure.tenantId
    };
    emit('update', payload);
  } else {
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
  }
  close();
}
</script>
