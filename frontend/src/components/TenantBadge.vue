<template>
  <v-chip
    v-if="label"
    class="tenant-badge"
    color="primary"
    variant="tonal"
    size="small"
    prepend-icon="mdi-domain"
    :title="label"
    data-testid="tenant-badge"
  >
    <span class="tenant-badge__text">{{ label }}</span>
  </v-chip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTenantStore } from '@/stores/useTenantStore'

const tenantStore = useTenantStore()
const label = computed(() => tenantStore.tenantLabel)
</script>

<style scoped>
.tenant-badge {
  max-width: 50vw;
}
.tenant-badge__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
