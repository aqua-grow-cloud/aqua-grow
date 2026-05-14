<template>
  <div>
    <v-card flat>
      <v-card-title class="d-flex align-center">
        Telegram Bot Settings
      </v-card-title>
      <v-card-subtitle class="pb-2">
        Assign Telegram Chat IDs to employees so they can interact with the bot.
      </v-card-subtitle>

      <v-card-text>
        <v-data-table
          :headers="headers"
          :items="employees || []"
          :loading="isLoading"
          hover
        >
          <template v-slot:item.telegramChatId="{ item }">
            <span v-if="item.telegramChatId" class="text-mono">{{ item.telegramChatId }}</span>
            <span v-else class="text-grey text-caption">Not set</span>
          </template>

          <template v-slot:item.actions="{ item }">
            <v-btn
              icon="mdi-pencil"
              variant="text"
              size="small"
              color="primary"
              @click="openDialog(item)"
            ></v-btn>
          </template>

          <template v-slot:no-data>
            <div class="pa-4 text-center">
              <v-icon size="64" color="grey-lighten-1">mdi-account-group</v-icon>
              <p class="text-grey">No employees found.</p>
            </div>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <!-- Edit Dialog -->
    <v-dialog v-model="dialog" max-width="480px" persistent>
      <v-card>
        <v-card-title>Update Telegram Chat ID</v-card-title>
        <v-card-subtitle class="pt-1 pb-0">{{ editedEmployee?.name }}</v-card-subtitle>
        <v-card-text class="pt-4">
          <v-text-field
            v-model="telegramChatId"
            label="Telegram Chat ID"
            placeholder="e.g. 123456789"
            hint="The user can get their Chat ID by messaging @userinfobot on Telegram."
            persistent-hint
            clearable
          ></v-text-field>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="closeDialog">Cancel</v-btn>
          <v-btn
            color="primary"
            :loading="updateMutation.isPending.value"
            @click="save"
          >
            Save
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue';
import {type Employee, employeeService} from './api/employeeService';

const { data: employees, isLoading } = employeeService.useGetAll();
const updateMutation = employeeService.useUpdate();

const headers = [
  { title: 'Name', key: 'name', sortable: true },
  { title: 'Role', key: 'role', sortable: true },
  { title: 'Telegram Chat ID', key: 'telegramChatId', sortable: false },
  { title: 'Actions', key: 'actions', sortable: false, align: 'end' as const },
];

const dialog = ref(false);
const editedEmployee = ref<Employee | null>(null);
const telegramChatId = ref<string>('');

function openDialog(employee: Employee) {
  editedEmployee.value = { ...employee };
  telegramChatId.value = employee.telegramChatId || '';
  dialog.value = true;
}

function closeDialog() {
  dialog.value = false;
  editedEmployee.value = null;
  telegramChatId.value = '';
}

async function save() {
  if (!editedEmployee.value?.id) return;
  await updateMutation.mutateAsync({
    id: editedEmployee.value.id,
    data: { ...editedEmployee.value, telegramChatId: telegramChatId.value || undefined }
  });
  closeDialog();
}
</script>
