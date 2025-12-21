<template>
  <v-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" max-width="500px">
    <v-card>
      <v-card-title>
        <span class="text-h5">{{ isEdit ? 'Edit Pond' : 'New Pond' }}</span>
      </v-card-title>

      <v-card-text>
        <v-container>
          <v-row>
            <v-col cols="12">
              <v-text-field
                v-model="form.name"
                label="Pond Name"
                variant="outlined"
                density="compact"
                hide-details="auto"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-select
                v-model="form.type"
                :items="['Earthen', 'Lined', 'Tank', 'Biofloc']"
                label="Type"
                variant="outlined"
                density="compact"
              ></v-select>
            </v-col>
            <v-col cols="12" sm="6">
              <v-select
                v-model="form.status"
                :items="['Active', 'Inactive', 'Maintenance']"
                label="Status"
                variant="outlined"
                density="compact"
              ></v-select>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.area"
                label="Area (m²)"
                type="number"
                variant="outlined"
                density="compact"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="form.depth"
                label="Depth (m)"
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
        <v-btn color="blue-darken-1" variant="text" @click="save">
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import type { Pond, PondType, PondStatus } from '../types';

const props = defineProps<{
  modelValue: boolean;
  item?: Pond; // If provided, we are in edit mode
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'save', pond: Omit<Pond, 'id'> | Pond): void;
}>();

const isEdit = computed(() => !!props.item);

const defaultForm = {
  name: '',
  type: 'Earthen' as PondType,
  status: 'Inactive' as PondStatus,
  area: 0,
  depth: 0,
};

const form = ref({ ...defaultForm });

watch(() => props.item, (newItem) => {
  if (newItem) {
    form.value = { ...newItem };
  } else {
    form.value = { ...defaultForm };
  }
}, { immediate: true });

function close() {
  emit('update:modelValue', false);
  form.value = { ...defaultForm };
}

function save() {
  emit('save', { ...form.value });
  close();
}
</script>
