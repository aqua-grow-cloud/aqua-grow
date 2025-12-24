<template>
  <v-container fluid>
    <PageHeader 
        title="Supplier Management" 
        action-label="Add Supplier" 
        @action="openDialog" 
    />

    <v-card variant="outlined" class="rounded-lg mt-4">
      <v-data-table
        :headers="headers"
        :items="suppliers || []"
        :loading="isLoading"
        class="elevation-0"
      >
        <template v-slot:item.feedTypes="{ item }">
            <v-chip
                v-for="type in item.feedTypes"
                :key="type"
                size="x-small"
                class="mr-1 mb-1"
                color="primary"
                variant="flat"
            >
                {{ type }}
            </v-chip>
        </template>
        
        <template v-slot:item.actions="{ item }">
          <v-btn icon size="small" variant="text" color="primary" @click="editItem(item)">
            <v-icon>mdi-pencil</v-icon>
          </v-btn>
          <v-btn icon size="small" variant="text" color="error" @click="deleteItem(item)">
            <v-icon>mdi-delete</v-icon>
          </v-btn>
        </template>
      </v-data-table>
    </v-card>

    <!-- Supplier Dialog -->
    <v-dialog v-model="dialog" max-width="600px">
      <v-card>
        <v-card-title>
          <span class="text-h5">{{ form.id ? 'Edit Supplier' : 'New Supplier' }}</span>
        </v-card-title>
        <v-card-text>
          <v-container>
            <v-form ref="formRef" @submit.prevent="save">
                <v-row>
                <v-col cols="12">
                    <v-text-field
                    v-model="form.name"
                    label="Supplier Name"
                    variant="outlined"
                    density="compact"
                    required
                    ></v-text-field>
                </v-col>
                <v-col cols="12" sm="6">
                    <v-text-field
                    v-model="form.phone"
                    label="Phone"
                    variant="outlined"
                    density="compact"
                    ></v-text-field>
                </v-col>
                <v-col cols="12" sm="6">
                    <v-text-field
                    v-model="form.whatsappNumber"
                    label="WhatsApp Number"
                    variant="outlined"
                    density="compact"
                    hint="Include country code if possible"
                    ></v-text-field>
                </v-col>
                <v-col cols="12">
                    <v-text-field
                    v-model="form.address"
                    label="Address"
                    variant="outlined"
                    density="compact"
                    ></v-text-field>
                </v-col>
                <v-col cols="12">
                    <v-combobox
                    v-model="form.feedTypes"
                    label="Feed Types Supplied"
                    chips
                    multiple
                    variant="outlined"
                    density="compact"
                    :items="['Starter', 'Grower', 'Finisher', 'Floating', 'Sinking']"
                    ></v-combobox>
                </v-col>
                </v-row>
            </v-form>
          </v-container>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="blue-darken-1" variant="text" @click="close">Cancel</v-btn>
          <v-btn color="blue-darken-1" variant="text" @click="save">Save</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import PageHeader from '../components/PageHeader.vue';
import { supplierService } from '../services/api';
import type { Supplier } from '../api/manual/types';

const headers = [
  { title: 'Name', key: 'name' },
  { title: 'Phone', key: 'phone' },
  { title: 'WhatsApp', key: 'whatsappNumber' },
  { title: 'Address', key: 'address' },
  { title: 'Feed Types', key: 'feedTypes' },
  { title: 'Actions', key: 'actions', sortable: false },
];

const { data: suppliers, isLoading } = supplierService.useGetAll();
const createMutation = supplierService.useCreate();
const updateMutation = supplierService.useUpdate();
const deleteMutation = supplierService.useDelete();

const dialog = ref(false);
const defaultForm: Supplier = {
    name: '',
    phone: '',
    whatsappNumber: '',
    address: '',
    feedTypes: []
};
const form = ref<Supplier>({ ...defaultForm });

function openDialog() {
    form.value = { ...defaultForm };
    dialog.value = true;
}

function editItem(item: Supplier) {
    form.value = { ...item };
    dialog.value = true;
}

async function deleteItem(item: Supplier) {
    if (!item.id) return;
    if (confirm(`Are you sure you want to delete ${item.name}?`)) {
        await deleteMutation.mutateAsync(item.id);
    }
}

function close() {
    dialog.value = false;
}

async function save() {
    if (!form.value.name) return; // Simple validation
    
    if (form.value.id) {
        await updateMutation.mutateAsync({ id: form.value.id, data: form.value });
    } else {
        await createMutation.mutateAsync(form.value);
    }
    close();
}
</script>
