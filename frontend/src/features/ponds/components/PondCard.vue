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
      <div v-if="activeBatches.length > 0" class="mt-3">
        <div class="text-caption text-grey mb-1">Active Batches:</div>
        <v-chip
          v-for="batch in activeBatches"
          :key="batch.id"
          size="small"
          color="primary"
          variant="tonal"
          class="mr-1 mb-1"
          @click="navigateToBatch(batch.id)"
          style="cursor: pointer"
        >
          <v-icon start size="small">mdi-fish</v-icon>
          {{ batch.name || 'Unnamed' }}
        </v-chip>
      </div>
      <div v-else class="text-caption text-grey mt-2">
        No active batches
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
import { useRouter } from 'vue-router';
import type { Pond, Batch } from '../../../api/generated/model';

const props = defineProps<{
  pond: Pond;
  batches?: Batch[];
  canManage?: boolean;
}>();

defineEmits<{
  (e: 'edit', pond: Pond): void
  (e: 'delete', id?: string): void
  (e: 'stock', id?: string): void
}>();

const router = useRouter();

const statusColor = computed(() => {
  switch (props.pond.status) {
    case 'Active': return 'success';
    case 'Maintenance': return 'warning';
    case 'Inactive': return 'grey';
    default: return 'primary';
  }
});

const activeBatches = computed(() => {
  if (!props.batches || !props.pond.id) return [];
  
  return props.batches.filter(batch => 
    batch.status === 'Active' && 
    batch.fishSeeds?.some(seed => seed.pondId === props.pond.id)
  );
});

function navigateToBatch(batchId?: string) {
  if (batchId) {
    router.push({ path: '/batches', query: { pondId: props.pond.id } });
  }
}
</script>
