<template>
  <v-app>
    <v-navigation-drawer
      v-model="drawer"
      app
      color="#0F172A"
      theme="dark" 
      :mobile-breakpoint="960"
      width="260"
    >
      <v-list-item class="py-4 px-4">
        <template v-slot:prepend>
           <v-avatar color="primary" size="32" class="mr-2">
             <span class="text-white font-weight-bold">AG</span>
           </v-avatar>
        </template>
        <v-list-item-title class="text-subtitle-1 font-weight-bold text-white">Aqua Grow</v-list-item-title>
      </v-list-item>

      <v-divider class="border-opacity-10 mb-4"></v-divider>

      <v-list density="compact" nav class="px-2">
        <v-list-item rounded="lg" prepend-icon="mdi-view-dashboard-outline" title="Dashboard" to="/" active-class="bg-primary text-white"></v-list-item>
        <v-list-item v-if="store.isManager" rounded="lg" prepend-icon="mdi-water-outline" title="Ponds" to="/ponds" active-class="bg-primary text-white"></v-list-item>
        <v-list-item v-if="store.isManager" rounded="lg" prepend-icon="mdi-fishbowl-outline" title="Batches" to="/batches" active-class="bg-primary text-white"></v-list-item>
        <v-list-item v-if="store.isOperator" rounded="lg" prepend-icon="mdi-clipboard-list-outline" title="Operations" to="/operations" active-class="bg-primary text-white"></v-list-item>
        <v-list-item v-if="store.isManager" rounded="lg" prepend-icon="mdi-scale-balance" title="Harvest" to="/harvest" active-class="bg-primary text-white"></v-list-item>
        <v-list-item v-if="store.isManager" rounded="lg" prepend-icon="mdi-cash-multiple" title="Payroll" to="/payroll" active-class="bg-primary text-white"></v-list-item>
        <v-list-item v-if="store.isManager" rounded="lg" prepend-icon="mdi-finance" title="Finances" to="/finance" active-class="bg-primary text-white"></v-list-item>
        <div v-if="store.isAdmin" class="mt-4 mb-2 text-caption text-grey-lighten-1 px-4 font-weight-bold">SETTINGS</div>
        <v-list-item v-if="store.isAdmin" rounded="lg" prepend-icon="mdi-cog-outline" title="Users & Roles" to="/settings" active-class="bg-primary text-white"></v-list-item>
      </v-list>

      <template v-slot:append>
        <v-divider class="border-opacity-10"></v-divider>
        <div class="pa-4">
          <v-select
            v-model="store.currentUser.role"
            :items="['Admin', 'Manager', 'Operator']"
            label="Simulate Role"
            density="compact"
            variant="outlined"
            hide-details
            class="mb-4 text-white"
            theme="dark"
          ></v-select>
          <v-list-item class="pa-0">
            <template v-slot:prepend>
               <v-avatar color="primary" size="32">{{ store.currentUser.role[0] }}</v-avatar>
            </template>
            <v-list-item-title class="text-body-2 text-white">{{ store.currentUser.role }}</v-list-item-title>
             <template v-slot:append>
               <v-btn icon="mdi-logout" variant="text" size="small" color="grey" @click="logout"></v-btn>
             </template>
          </v-list-item>
        </div>
      </template>
    </v-navigation-drawer>

    <v-app-bar color="surface" elevation="0" class="border-b">
      <v-app-bar-nav-icon @click="drawer = !drawer" color="grey-darken-1"></v-app-bar-nav-icon>
      <v-spacer></v-spacer>
    </v-app-bar>

    <v-main>
      <router-view></router-view>
    </v-main>

    <!-- Global Notification Snackbar -->
    <v-snackbar
      v-model="store.isGlobalErrorVisible"
      :color="store.globalErrorType"
      location="top right"
      timeout="5000"
    >
      {{ store.globalError }}
      <template v-slot:actions>
        <v-btn variant="text" @click="store.clearNotification">Close</v-btn>
      </template>
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
import { inject, ref } from 'vue'
import { useAquaStore } from './stores/useAquaStore'

const store = useAquaStore()
// import Keycloak from 'keycloak-js' // Type only

const keycloak = inject('keycloak') as any
const drawer = ref(true)

const logout = () => {
  keycloak.logout()
}
</script>


