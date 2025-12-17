<template>
  <v-app>
    <v-app-bar color="primary" density="compact">
      <v-app-bar-title>Aqua Grow</v-app-bar-title>
      <v-spacer></v-spacer>
      <v-btn to="/" variant="text">Dashboard</v-btn>
      <v-btn to="/ponds" variant="text">Ponds</v-btn>
      <v-btn @click="logout" icon>
        <v-icon>mdi-logout</v-icon>
      </v-btn>
    </v-app-bar>

    <v-main>
      <router-view></router-view>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { inject } from 'vue'
// import Keycloak from 'keycloak-js' // Type only

const keycloak = inject('keycloak') as any

const logout = () => {
  keycloak.logout()
}
</script>
