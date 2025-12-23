import { computed } from 'vue';
import { useQueryClient } from '@tanstack/vue-query';
import { batchService, type Batch } from '../services/api';
import { useBatchStore } from '../stores/useBatchStore';

/**
 * Batches Composable
 * Provides reusable batch operations with loading/error states
 */
export function useBatches() {
    const queryClient = useQueryClient();
    const batchStore = useBatchStore();

    // Queries
    const { data: batches, isLoading, error, refetch } = batchService.useGetAll();

    // Mutations
    const createBatch = batchService.useCreate();
    const updateBatch = batchService.useUpdate();

    // Computed
    const activeBatches = computed(() =>
        batches.value?.filter((b: Batch) => b.status === 'Active') || []
    );

    const harvestedBatches = computed(() =>
        batches.value?.filter((b: Batch) => b.status === 'Harvested') || []
    );

    // Actions
    async function create(batchData: any) {
        await createBatch.mutateAsync({ data: batchData });
        queryClient.invalidateQueries({ queryKey: ['api', 'batches'] });
        queryClient.invalidateQueries({ queryKey: ['api', 'ponds'] }); // Refresh ponds to show currentBatchId
    }

    async function update(id: string, batchData: any) {
        await updateBatch.mutateAsync({ id, data: batchData });
        queryClient.invalidateQueries({ queryKey: ['api', 'batches'] });
    }

    function getBatchById(id?: string) {
        if (!id || !batches.value) return undefined;
        return batches.value.find((b: Batch) => b.id === id);
    }

    return {
        // Data
        batches,
        activeBatches,
        harvestedBatches,

        // State
        isLoading,
        error,

        // Mutations
        isCreating: createBatch.isPending,
        isUpdating: updateBatch.isPending,

        // Actions
        create,
        update,
        refetch,
        getBatchById,

        // Store
        selectedBatchId: computed(() => batchStore.selectedBatchId),
        selectBatch: batchStore.selectBatch,
        clearSelection: batchStore.clearSelection,
    };
}
