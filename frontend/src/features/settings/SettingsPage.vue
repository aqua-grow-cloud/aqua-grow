<template>
  <v-container fluid>
    <PageHeader title="Settings" />

    <v-tabs v-model="tab" color="primary">
      <v-tab value="users">Users & Roles</v-tab>
      <v-tab value="fish-types">Fish Types</v-tab>
    </v-tabs>

    <v-window v-model="tab" class="mt-4">
      <v-window-item value="users">
        <v-row v-if="!store.isAdmin">
          <v-col cols="12">
            <v-alert type="error" variant="tonal" title="Access Denied">
              Only Administrators can manage users and roles.
            </v-alert>
          </v-col>
        </v-row>

        <v-card v-else>
          <v-card-title>
            User Management
            <v-chip size="small" color="warning" class="ml-2">Mock Data</v-chip>
          </v-card-title>
          <v-card-text>
            <v-table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>Admin User</td>
                  <td><v-chip color="purple" size="small">Manager</v-chip></td>
                  <td>Active</td>
                  <td><v-btn size="small" variant="text">Edit</v-btn></td>
                </tr>
                <tr>
                  <td>John Doe</td>
                  <td><v-chip color="blue" size="small">Operator</v-chip></td>
                  <td>Active</td>
                  <td><v-btn size="small" variant="text">Edit</v-btn></td>
                </tr>
                 <tr>
                  <td>Jane Smith</td>
                  <td><v-chip color="blue" size="small">Operator</v-chip></td>
                  <td>Active</td>
                  <td><v-btn size="small" variant="text">Edit</v-btn></td>
                </tr>
              </tbody>
            </v-table>
          </v-card-text>
          <v-card-actions>
            <v-btn color="primary" prepend-icon="mdi-account-plus">Add User</v-btn>
          </v-card-actions>
        </v-card>
      </v-window-item>

      <v-window-item value="fish-types">
        <FishTypesSettings />
      </v-window-item>
    </v-window>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAquaStore } from '../../stores/useAquaStore';
import PageHeader from '../../components/PageHeader.vue';
import FishTypesSettings from './FishTypesSettings.vue';

const store = useAquaStore();
const tab = ref('users');
</script>
