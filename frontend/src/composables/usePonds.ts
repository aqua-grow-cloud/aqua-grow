import { computed } from 'vue';
import { useQueryClient } from '@tanstack/vue-query';
import { pondService, type Pond } from '../services/api';
import { usePondStore } from '../stores/usePondStore';

/**
 * Ponds Composable
 * Provides reusable pond operations with loading/error states
 */
export function usePonds() {
    const queryClient = useQueryClient();
    const pondStore = usePondStore();

    // Queries
    const { data: ponds, isLoading, error, refetch } = pondService.useGetAll();

    // Mutations
    const createPond = pondService.useCreate();
    const updatePond = pondService.useUpdate();
    const deletePond = pondService.useDelete();

    // Computed
    const activePonds = computed(() =>
        ponds.value?.filter((p: Pond) => p.status === 'Active') || []
    );

    const inactivePonds = computed(() =>
        ponds.value?.filter((p: Pond) => p.status === 'Inactive') || []
    );

    // Actions
    async function create(pondData: any) {
        await createPond.mutateAsync({ data: pondData });
        queryClient.invalidateQueries({ queryKey: ['api', 'ponds'] });
    }

    async function update(id: string, pondData: any) {
        await updatePond.mutateAsync({ id, data: pondData });
        queryClient.invalidateQueries({ queryKey: ['api', 'ponds'] });
    }

    async function remove(id?: string) {
        if (!id) return;
        await deletePond.mutateAsync({ id });
        queryClient.invalidateQueries({ queryKey: ['api', 'ponds'] });
    }

    function getPondById(id?: string) {
        if (!id || !ponds.value) return undefined;
        return ponds.value.find((p: Pond) => p.id === id);
    }

    return {
        // Data
        ponds,
        activePonds,
        inactivePonds,

        // State
        isLoading,
        error,

        // Mutations
        isCreating: createPond.isPending,
        isUpdating: updatePond.isPending,
        isDeleting: deletePond.isPending,

        // Actions
        create,
        update,
        remove,
        refetch,
        getPondById,

        // Store
        selectedPondId: computed(() => pondStore.selectedPondId),
        selectPond: pondStore.selectPond,
        clearSelection: pondStore.clearSelection,
    };
}
