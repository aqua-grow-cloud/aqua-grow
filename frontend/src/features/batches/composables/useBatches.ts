import type {MaybeRef} from 'vue';
import {computed} from 'vue';
import {useQueryClient} from '@tanstack/vue-query';
import {type Batch, batchService} from '../api/batchService';
import {useBatchStore} from '../stores/useBatchStore';
import type {GetAllBatchesParams} from '@/api/generated/model';

/**
 * Batches Composable
 * Provides reusable batch operations with loading/error states
 */
export function useBatches(params?: MaybeRef<GetAllBatchesParams>) {
    const queryClient = useQueryClient();
    const batchStore = useBatchStore();

    // Queries
    const { data: batches, isLoading, error, refetch } = batchService.useGetAll(params);

    // Mutations
    const createBatch = batchService.useCreate();
    const updateBatch = batchService.useUpdate();
    const addSeedMutation = batchService.useAddSeed();
    const moveSeedMutation = batchService.useMoveSeed();
    const logMortalityMutation = batchService.useLogMortality();

    // Computed
    const activeBatches = computed(() =>
        batches.value?.filter((b: Batch) => b.status === 'Active') || []
    );

    const harvestedBatches = computed(() =>
        batches.value?.filter((b: Batch) => b.status === 'Harvested') || []
    );

    // Actions
    async function create(batchData: any) {
        const result = await createBatch.mutateAsync({ data: batchData });
        queryClient.invalidateQueries({ queryKey: ['api', 'batches'] });
        queryClient.invalidateQueries({ queryKey: ['api', 'ponds'] }); // Refresh ponds to show currentBatchId
        return result;
    }

    async function update(id: string, batchData: any) {
        await updateBatch.mutateAsync({ id, data: batchData });
        queryClient.invalidateQueries({ queryKey: ['api', 'batches'] });
    }

    async function addSeed(batchId: string, seed: any) {
        await addSeedMutation.mutateAsync({ batchId, seed });
        queryClient.invalidateQueries({ queryKey: ['api', 'batches'] });
    }

    async function moveSeed(payload: { batchId: string; seedId: string; targetPondId: string; quantity?: number }) {
        await moveSeedMutation.mutateAsync(payload);
        queryClient.invalidateQueries({ queryKey: ['api', 'batches'] });
        queryClient.invalidateQueries({ queryKey: ['api', 'ponds'] });
    }

    async function logMortality(payload: { batchId: string; seedId: string; quantity: number; date?: string; note?: string }) {
        await logMortalityMutation.mutateAsync(payload);
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
        isAddingSeed: addSeedMutation.isPending,

        // Actions
        // Actions
        create,
        update,
        addSeed,
        moveSeed,
        logMortality,
        refetch,
        getBatchById,

        // Store
        selectedBatchId: computed(() => batchStore.selectedBatchId),
        selectBatch: batchStore.selectBatch,
        clearSelection: batchStore.clearSelection,
    };
}
