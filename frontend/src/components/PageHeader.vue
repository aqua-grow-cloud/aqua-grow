<template>
  <div class="d-flex align-center flex-wrap mb-2">
    <h1 class="text-h5 font-weight-bold text-grey-darken-3 mr-4">{{ title }}</h1>
    <v-spacer></v-spacer>
    
    <!-- Slot for custom actions -->
    <slot name="actions"></slot>
    
    <!-- Fallback to single action button if no slot provided -->
    <v-btn
      v-if="!$slots.actions && actionLabel"
      color="primary"
      variant="flat"
      prepend-icon="mdi-plus"
      @click="$emit('action')"
      class="text-capitalize font-weight-bold"
      size="small"
    >
      {{ actionLabel }}
    </v-btn>
  </div>
  <v-divider class="mb-3 border-opacity-25"></v-divider>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  actionLabel?: string;
}>();

defineEmits<{
  (e: 'action'): void;
}>();
</script>
