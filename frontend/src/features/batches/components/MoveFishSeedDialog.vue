<template>
  <v-dialog v-model="internalValue" max-width="500px">
    <v-card>
      <v-card-title class="bg-primary text-white py-4 px-6 d-flex align-center">
        <v-icon left class="mr-2">mdi-truck-delivery</v-icon>
        <span>Move Fish Seed</span>
        <v-spacer></v-spacer>
        <v-btn icon="mdi-close" variant="text" density="compact" @click="internalValue = false"></v-btn>
      </v-card-title>

      <v-card-text class="pa-6">
        <v-alert
          v-if="error"
          type="error"
          variant="tonal"
          closable
          class="mb-4"
          @click:close="error = ''"
        >
          {{ error }}
        </v-alert>

        <v-form ref="form" v-model="valid">
          <div class="mb-4">
            <div class="text-subtitle-2 text-grey-600 mb-1">Source Seed Detail</div>
            <v-card variant="outlined" density="compact" class="pa-3 bg-slate-50">
              <v-row dense>
                <v-col cols="6">
                  <div class="text-caption text-grey">Type</div>
                  <div class="font-weight-medium">{{ seed?.fishTypeName }}</div>
                </v-col>
                <v-col cols="6">
                  <div class="text-caption text-grey">Current Quantity</div>
                  <div class="font-weight-medium">{{ seed?.quantity?.toLocaleString() }}</div>
                </v-col>
              </v-row>
            </v-card>
          </div>

          <v-row>
            <v-col cols="12">
              <v-select
                v-model="targetPondId"
                :items="availablePonds"
                item-title="name"
                item-value="id"
                label="Destination Pond *"
                prepend-inner-icon="mdi-waves"
                :rules="[v => !!v || 'Destination pond is required']"
                variant="outlined"
              ></v-select>
            </v-col>
            <v-col cols="12">
              <v-text-field
                v-model.number="quantity"
                label="Quantity to Move *"
                type="number"
                prepend-inner-icon="mdi-counter"
                :rules="[
                  v => !!v || 'Quantity is required',
                  v => v > 0 || 'Quantity must be greater than 0',
                  v => v <= (seed?.quantity || 0) || 'Cannot move more than available'
                ]"
                hint="Leave as total to perform a full move"
                persistent-hint
                variant="outlined"
              ></v-text-field>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>

      <v-divider></v-divider>

      <v-card-actions class="pa-4 bg-slate-50">
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="internalValue = false">Cancel</v-btn>
        <v-btn
          color="primary"
          variant="elevated"
          :loading="loading"
          :disabled="!valid"
          @click="handleSave"
        >
          Transfer Fish
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { FishSeed, Pond } from '../../../api/generated/model';

interface Props {
  modelValue: boolean;
  seed: any | null; // The flattened seed object from the table
  ponds: Pond[];
  loading?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits(['update:modelValue', 'save']);

const internalValue = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const form = ref<any>(null);
const valid = ref(false);
const error = ref('');
const targetPondId = ref<string | null>(null);
const quantity = ref<number>(0);

const availablePonds = computed(() => {
  return props.ponds.filter(p => p.id !== props.seed?.pondId);
});

watch(() => props.modelValue, (newVal) => {
  if (newVal && props.seed) {
    targetPondId.value = null;
    quantity.value = props.seed.quantity || 0;
    error.value = '';
    if (form.value) form.value.resetValidation();
  }
});

async function handleSave() {
  const { valid: isFormValid } = await form.value.validate();
  if (!isFormValid) return;

  if (!props.seed?.batchId) {
     error.value = 'Batch ID missing';
     return;
  }

  emit('save', {
    batchId: props.seed.batchId,
    seedId: props.seed.id,
    targetPondId: targetPondId.value,
    quantity: quantity.value
  });
}
</script>
