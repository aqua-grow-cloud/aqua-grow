<template>
  <v-app>
    <v-navigation-drawer
      v-model="drawer"
      app
      class="bg-gradient-primary"
      theme="dark" 
      :mobile-breakpoint="960"
    >
      <!-- Gradient Background Overlay -->
      <template v-slot:image>
         <div class="fill-height gradient-bg"></div>
      </template>

      <v-list-item class="py-4">
        <template v-slot:prepend>
           <v-avatar color="white" size="40">
             <v-icon color="primary" icon="mdi-water"></v-icon>
           </v-avatar>
        </template>
        <v-list-item-title class="text-h6 font-weight-bold ml-2">Aqua Grow</v-list-item-title>
        <v-list-item-subtitle class="ml-2">Farm Manager</v-list-item-subtitle>
      </v-list-item>

      <v-divider class="border-opacity-25"></v-divider>

      <v-list density="comfortable" nav class="mt-2">
        <v-list-item rounded="xl" prepend-icon="mdi-view-dashboard" title="Dashboard" to="/" active-class="bg-white text-primary"></v-list-item>
        <v-list-item rounded="xl" prepend-icon="mdi-water" title="Ponds" to="/ponds" active-class="bg-white text-primary"></v-list-item>
        <v-list-item rounded="xl" prepend-icon="mdi-fishbowl" title="Batches" to="/batches" active-class="bg-white text-primary"></v-list-item>
        <v-list-item rounded="xl" prepend-icon="mdi-clipboard-list" title="Operations" to="/operations" active-class="bg-white text-primary"></v-list-item>
        <v-list-item rounded="xl" prepend-icon="mdi-Scale-balance" title="Harvest" to="/harvest" active-class="bg-white text-primary"></v-list-item>
        <v-list-item rounded="xl" prepend-icon="mdi-cog" title="Settings" to="/settings" active-class="bg-white text-primary"></v-list-item>
      </v-list>

      <template v-slot:append>
        <div class="pa-4">
          <v-btn block color="white" variant="outlined" @click="logout" class="text-none">
            Logout
          </v-btn>
        </div>
      </template>
    </v-navigation-drawer>

    <v-app-bar color="primary" density="compact">
      <v-app-bar-nav-icon @click="drawer = !drawer"></v-app-bar-nav-icon>
      <v-app-bar-title>Aqua Grow</v-app-bar-title>
    </v-app-bar>

    <v-main>
      <v-container fluid class="pa-3">
        <router-view></router-view>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { inject, ref } from 'vue'
// import Keycloak from 'keycloak-js' // Type only

const keycloak = inject('keycloak') as any
const drawer = ref(true)

const logout = () => {
  keycloak.logout()
}
</script>

<style scoped>
.gradient-bg {
  background: linear-gradient(180deg, #00695C 0%, #004D40 100%);
  opacity: 0.95;
}
</style>
