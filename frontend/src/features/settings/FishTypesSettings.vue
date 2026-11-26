<template>
  <div>
    <v-card flat>
      <v-card-title class="d-flex align-center">
        Fish Species
        <v-spacer></v-spacer>
        <v-btn
          color="primary"
          prepend-icon="mdi-plus"
          @click="openDialog()"
          :loading="createMutation.isPending.value"
        >
          Add Species
        </v-btn>
      </v-card-title>

      <v-card-text>
        <v-data-table
          :headers="headers"
          :items="fishTypes || []"
          :loading="isLoading"
          hover
        >
          <template v-slot:item.cultivationType="{ item }">
            <v-chip
              v-if="item.cultivationType"
              size="small"
              :color="cultivationTypeColor(item.cultivationType)"
              variant="tonal"
            >
              {{ item.cultivationType }}
            </v-chip>
            <span v-else class="text-grey-lighten-1">—</span>
          </template>

          <template v-slot:item.actions="{ item }">
            <v-btn
              icon="mdi-pencil"
              variant="text"
              size="small"
              color="primary"
              @click="openDialog(item)"
            ></v-btn>
            <v-btn
              icon="mdi-delete"
              variant="text"
              size="small"
              color="error"
              @click="confirmDelete(item)"
              :loading="deleteMutation.isPending.value && deletingId === item.id"
            ></v-btn>
          </template>

          <template v-slot:no-data>
            <div class="pa-4 text-center">
              <v-icon size="64" color="grey-lighten-1">mdi-fish</v-icon>
              <p class="text-grey">No fish species defined yet.</p>
              <v-btn color="primary" variant="text" @click="openDialog()">
                Create your first species
              </v-btn>
            </div>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <!-- Add/Edit Dialog -->
    <v-dialog v-model="dialog" max-width="500px" persistent>
      <v-form @submit.prevent="save" ref="form">
        <v-card>
          <v-card-title>{{ isEditing ? 'Edit' : 'New' }} Fish Species</v-card-title>
          <v-card-text>
            <v-text-field
              v-model="editedItem.name"
              label="Species Name"
              placeholder="e.g. Tilapia, Catfish"
              required
              :rules="[v => !!v || 'Name is required']"
            ></v-text-field>
            <v-text-field
              v-model="editedItem.scientificName"
              label="Scientific Name"
              placeholder="e.g. Oreochromis niloticus"
            ></v-text-field>
            <v-select
              v-model="editedItem.cultivationType"
              label="Cultivation Type"
              :items="cultivationTypes"
              clearable
            ></v-select>
          </v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn variant="text" @click="closeDialog">Cancel</v-btn>
            <v-btn
              color="primary"
              type="submit"
              :loading="createMutation.isPending.value || updateMutation.isPending.value"
            >
              Save
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-form>
    </v-dialog>

    <!-- Delete Confirmation -->
    <v-dialog v-model="deleteDialog" max-width="400px">
      <v-card>
        <v-card-title class="text-h5">Delete Species?</v-card-title>
        <v-card-text>
          Are you sure you want to delete <strong>{{ itemToDelete?.name }}</strong>?
          This may affect existing records referencing this species.
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="deleteDialog = false">Cancel</v-btn>
          <v-btn
            color="error"
            @click="doDelete"
            :loading="deleteMutation.isPending.value"
          >
            Delete
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {type FishType, fishTypeService} from './api/fishTypeService';

const { data: fishTypes, isLoading } = fishTypeService.useGetAll();
const createMutation = fishTypeService.useCreate();
const updateMutation = fishTypeService.useUpdate();
const deleteMutation = fishTypeService.useDelete();

const headers = [
  { title: 'Name', key: 'name', sortable: true },
  { title: 'Scientific Name', key: 'scientificName', sortable: true },
  { title: 'Cultivation Type', key: 'cultivationType', sortable: true },
  { title: 'Actions', key: 'actions', sortable: false, align: 'end' as const },
];

const cultivationTypes = ['FISH', 'SHRIMP', 'OTHER'];

function cultivationTypeColor(type: string): string {
  const colors: Record<string, string> = { FISH: 'blue', SHRIMP: 'orange', OTHER: 'grey' };
  return colors[type] ?? 'grey';
}

const dialog = ref(false);
const deleteDialog = ref(false);
const form = ref<any>(null);
const deletingId = ref<string | null>(null);
const itemToDelete = ref<FishType | null>(null);

const defaultItem: FishType = {
  name: '',
  scientificName: ''
};

const editedItem = ref<FishType>({ ...defaultItem });
const isEditing = computed(() => !!editedItem.value.id);

function openDialog(item?: FishType) {
  if (item) {
    editedItem.value = { ...item };
  } else {
    editedItem.value = { ...defaultItem };
  }
  dialog.value = true;
}

function closeDialog() {
  dialog.value = false;
  setTimeout(() => {
    editedItem.value = { ...defaultItem };
  }, 300);
}

async function save() {
  const { valid } = await form.value.validate();
  if (!valid) return;

  if (isEditing.value) {
    await updateMutation.mutateAsync({
      id: editedItem.value.id!,
      data: editedItem.value
    });
  } else {
    await createMutation.mutateAsync(editedItem.value);
  }
  closeDialog();
}

function confirmDelete(item: FishType) {
  itemToDelete.value = item;
  deleteDialog.value = true;
}

async function doDelete() {
  if (itemToDelete.value?.id) {
    deletingId.value = itemToDelete.value.id;
    await deleteMutation.mutateAsync(itemToDelete.value.id);
    deleteDialog.value = false;
    itemToDelete.value = null;
    deletingId.value = null;
  }
}
</script>
