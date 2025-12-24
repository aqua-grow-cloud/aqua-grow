<template>
  <v-dialog v-model="dialog" max-width="500">
    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon color="success" class="mr-2">mdi-whatsapp</v-icon>
        WhatsApp Order Message
      </v-card-title>
      <v-card-text>
        <v-alert v-if="loading" type="info" variant="tonal" class="mb-4">
          Generating message...
        </v-alert>

        <div v-else>
            <v-alert type="info" density="compact" variant="tonal" class="mb-4">
            Send this message to: <strong>{{ supplierPhone }}</strong>
            </v-alert>
            
            <v-textarea
            :model-value="message"
            readonly
            rows="12"
            variant="outlined"
            hide-details
            class="font-monospace"
            bg-color="grey-lighten-4"
            ></v-textarea>
        </div>
      </v-card-text>
      <v-card-actions>
        <v-spacer/>
        <v-btn @click="copyToClipboard" variant="elevated">
          <v-icon start>mdi-content-copy</v-icon>
          Copy
        </v-btn>
        <v-btn v-if="whatsappLink" :href="whatsappLink" target="_blank" color="success" variant="flat">
          <v-icon start>mdi-whatsapp</v-icon>
          Open WhatsApp
        </v-btn>
        <v-btn @click="dialog = false">Close</v-btn>
      </v-card-actions>
    </v-card>

    <v-snackbar v-model="snackbar" :timeout="2000" color="success">
        Message copied to clipboard!
    </v-snackbar>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { feedOrderService } from '../services/api';

const props = defineProps<{
  modelValue: boolean;
  orderId?: string;
  contactPerson?: string;
  location?: string;
}>();

const emit = defineEmits(['update:modelValue']);

const dialog = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const message = ref('');
const whatsappLink = ref('');
const supplierPhone = ref('');
const snackbar = ref(false);

const { data: messageData, isLoading, refetch } = feedOrderService.useGetOrderMessage(
    props.orderId, 
    props.contactPerson,
    props.location
);

// Watch for loading state
const loading = computed(() => isLoading.value);

watch(() => messageData.value, (newVal) => {
    if (newVal) {
        message.value = newVal.message;
        whatsappLink.value = newVal.whatsappLink;
        supplierPhone.value = newVal.supplierPhone;
    }
});

watch(() => props.orderId, () => {
    if (props.modelValue && props.orderId) {
        refetch();
    }
});

watch(() => props.modelValue, (isOpen) => {
    if (isOpen && props.orderId) {
        refetch();
    }
});

function copyToClipboard() {
  navigator.clipboard.writeText(message.value).then(() => {
    snackbar.value = true;
  });
}
</script>

<style scoped>
.font-monospace {
    font-family: monospace;
}
</style>
