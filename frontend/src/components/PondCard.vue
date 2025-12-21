<template>
  <v-card class="mx-auto border-opacity-75" variant="outlined">
    <v-card-item>
      <div class="d-flex justify-space-between align-center">
        <v-card-title>{{ pond.name }}</v-card-title>
        <v-chip :color="statusColor" size="small">{{ pond.status }}</v-chip>
      </div>
      <v-card-subtitle>{{ pond.type }} System</v-card-subtitle>
    </v-card-item>

    <v-card-text>
      <div class="d-flex align-center mb-2">
        <v-icon icon="mdi-ruler-square" class="mr-2" size="small"></v-icon>
        <span>{{ pond.area }} m²</span>
        <v-spacer></v-spacer>
        <v-icon icon="mdi-arrow-expand-vertical" class="mr-2" size="small"></v-icon>
        <span>{{ pond.depth }} m Depth</span>
      </div>
      <div v-if="pond.currentBatchId" class="text-caption text-primary mt-2">
        <v-icon icon="mdi-fish" size="small" class="mr-1"></v-icon>
        Active Batch: {{ pond.currentBatchId }}
      </div>
      <div v-else class="text-caption text-grey mt-2">
        No active batch
      </div>
    </v-card-text>

    <v-divider></v-divider>

    <v-card-actions v-if="canManage">
      <v-btn 
        v-if="pond.status === 'Active'"
        variant="tonal" 
        color="success" 
        prepend-icon="mdi-fish"
        @click="$emit('stock', pond.id)"
      >
        Stock
      </v-btn>
      <v-spacer></v-spacer>
      <v-btn variant="text" color="primary" @click="$emit('edit', pond)">
        Edit
      </v-btn>
      <v-spacer></v-spacer>
      <v-btn variant="text" color="error" @click="$emit('delete', pond.id)">
        Delete
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { Pond } from '../types';

const props = defineProps<{
  pond: Pond;
  canManage?: boolean;
}>();

defineEmits<{
  (e: 'edit', pond: Pond): void
  (e: 'delete', id: string): void
  (e: 'stock', id: string): void
}>();

const statusColor = computed(() => {
  switch (props.pond.status) {
    case 'Active': return 'success';
    case 'Maintenance': return 'warning';
    case 'Inactive': return 'grey';
    default: return 'primary';
  }
});
</script>
