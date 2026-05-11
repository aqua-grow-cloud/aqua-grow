<template>
  <v-dialog v-model="show" max-width="480px">
    <v-card class="rounded-lg">
      <v-card-title class="bg-error text-white px-6 py-4">
        <v-icon icon="mdi-skull-outline" class="mr-2"></v-icon>
        Log Mortality
      </v-card-title>

      <v-card-text class="pt-6">
        <div v-if="seed" class="text-body-2 text-grey mb-4">
          <span class="font-weight-bold">Seed:</span>
          {{ fishTypeName }} — {{ seed.quantity?.toLocaleString() }} pcs in pond {{ seed.pondId }}
        </div>

        <v-form ref="formRef" v-model="isFormValid" @submit.prevent="save">
          <v-row dense>
            <!-- Dead Count -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.quantity"
                label="Dead Count"
                type="number"
                variant="outlined"
                density="compact"
                min="1"
                :rules="[v => (v > 0) || 'Must be > 0']"
              ></v-text-field>
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

            <!-- Note / Cause -->
            <v-col cols="12">
              <v-text-field
                v-model="form.note"
                label="Cause / Note (optional)"
                variant="outlined"
                density="compact"
                placeholder="e.g. disease, low oxygen, handling"
              ></v-text-field>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>

      <v-card-actions class="px-6 pb-4">
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="show = false">Cancel</v-btn>
        <v-btn
          color="error"
          variant="flat"
          :loading="saving"
          :disabled="!isFormValid"
          @click="save"
        >
          Log Mortality
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import type {FishSeed} from '@/api/generated/model';
import type {FishType} from '@/api/manual/hooks';

const props = defineProps<{
  modelValue: boolean;
  batchId: string;
  seed: FishSeed | null;
  fishTypes: FishType[];
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  'saved': [payload: { batchId: string; seedId: string; quantity: number; date: string; note?: string }];
}>();

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

const formRef = ref();
const isFormValid = ref(false);
const saving = ref(false);

const today = new Date().toISOString().slice(0, 10);

const form = ref({
  quantity: 1,
  date: today,
  note: '',
});

// Reset form when dialog opens with a new seed
watch(() => props.modelValue, (open) => {
  if (open) {
    form.value = { quantity: 1, date: today, note: '' };
  }
});

const fishTypeName = computed(() => {
  if (!props.seed?.fishTypeId) return '-';
  return props.fishTypes.find(t => t.id === props.seed!.fishTypeId)?.name || props.seed.fishTypeId;
});

async function save() {
  const { valid } = await formRef.value.validate();
  if (!valid || !props.seed?.id) return;

  saving.value = true;
  try {
    emit('saved', {
      batchId: props.batchId,
      seedId: props.seed.id,
      quantity: form.value.quantity,
      date: form.value.date,
      note: form.value.note || undefined,
    });
    show.value = false;
  } finally {
    saving.value = false;
  }
}
</script>
