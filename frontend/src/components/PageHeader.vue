<template>
  <div class="d-flex align-center flex-wrap mb-2">
    <h1 class="text-h5 font-weight-bold text-grey-darken-3 mr-4">{{ title }}</h1>
    <v-spacer></v-spacer>
    <v-btn
      v-if="actionLabel"
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
