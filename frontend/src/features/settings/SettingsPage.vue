<template>
  <v-container fluid>
    <PageHeader title="Users & Roles" />

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
  </v-container>
</template>

<script setup lang="ts">
import { useAquaStore } from '../../stores/useAquaStore';
import PageHeader from '../../components/PageHeader.vue';
const store = useAquaStore();
// Static mock for now
</script>
