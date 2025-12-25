import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

/**
 * Batch Store
 * Manages batch state globally using Vue Query for data fetching
 */
export const useBatchStore = defineStore('batch', () => {
    // State
    const selectedBatchId = ref<string | null>(null);

    // Getters
    const selectedBatch = computed(() => selectedBatchId.value);

    // Actions
    function selectBatch(id: string | null) {
        selectedBatchId.value = id;
    }

    function clearSelection() {
        selectedBatchId.value = null;
    }

    return {
        // State
        selectedBatchId,

        // Getters
        selectedBatch,

        // Actions
        selectBatch,
        clearSelection,
    };
});
