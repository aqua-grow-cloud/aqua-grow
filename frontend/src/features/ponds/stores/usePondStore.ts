import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

/**
 * Pond Store
 * Manages pond state globally using Vue Query for data fetching
 */
export const usePondStore = defineStore('pond', () => {
    // State
    const selectedPondId = ref<string | null>(null);

    // Getters
    const selectedPond = computed(() => selectedPondId.value);

    // Actions
    function selectPond(id: string | null) {
        selectedPondId.value = id;
    }

    function clearSelection() {
        selectedPondId.value = null;
    }

    return {
        // State
        selectedPondId,

        // Getters
        selectedPond,

        // Actions
        selectPond,
        clearSelection,
    };
});
