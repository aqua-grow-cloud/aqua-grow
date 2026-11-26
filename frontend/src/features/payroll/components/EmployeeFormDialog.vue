<template>
  <v-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" max-width="500px">
    <v-card class="rounded-lg">
      <v-card-title class="px-6 pt-6">
        <span class="text-h5">{{ item ? 'Edit Employee' : 'Add Employee' }}</span>
      </v-card-title>

      <v-card-text class="px-6 pb-6">
        <v-form ref="form" v-model="isValid">
          <v-row>
            <v-col cols="12">
              <v-text-field
                v-model="payload.name"
                label="Full Name"
                variant="outlined"
                density="compact"
                hide-details="auto"
                :rules="[v => !!v || 'Name is required']"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-select
                v-model="payload.role"
                :items="['Manager', 'Operator', 'Biologist', 'Watchman', 'Other']"
                label="Role"
                variant="outlined"
                density="compact"
                hide-details="auto"
              ></v-select>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="payload.salary"
                label="Monthly Salary"
                type="number"
                prefix="₹"
                variant="outlined"
                density="compact"
                hide-details="auto"
                :rules="[v => v > 0 || 'Required']"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-select
                v-model="payload.status"
                :items="['Active', 'Inactive']"
                label="Status"
                variant="outlined"
                density="compact"
                hide-details="auto"
              ></v-select>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>

      <v-card-actions class="px-6 pb-6">
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="$emit('update:modelValue', false)">Cancel</v-btn>
        <v-btn color="primary" variant="flat" :disabled="!isValid" @click="save">Save</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { Employee } from '../../../api/generated/model';

const props = defineProps<{
  modelValue: boolean;
  item?: Employee;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'save', data: any): void;
}>();

const isValid = ref(false);
const payload = ref({
  name: '',
  role: 'Operator',
  salary: 0,
  status: 'Active' as 'Active' | 'Inactive',
});

watch(() => props.modelValue, (val) => {
  if (val && props.item) {
    payload.value = {
      name: props.item.name || '',
      role: props.item.role || 'Operator',
      salary: props.item.salary || 0,
      status: (props.item.status as 'Active' | 'Inactive') || 'Active',
    };
  } else if (val) {
    payload.value = {
      name: '',
      role: 'Operator',
      salary: 0,
      status: 'Active',
    };
  }
});

function save() {
  emit('save', { ...payload.value });
  emit('update:modelValue', false);
}
</script>
