import { harvestLogService } from '../api/harvestLogService';
import { useQueryClient } from '@tanstack/vue-query';

/**
 * Harvest Logs Composable
 * Provides reusable harvest log operations with loading/error states
 */
export function useHarvestLogs() {
    const queryClient = useQueryClient();

    // Queries
    const { data: harvestLogs, isLoading, error, refetch } = harvestLogService.useGetAll();

    // Mutations
    const recordHarvestMutation = harvestLogService.useRecord();

    // Actions
    async function recordHarvest(payload: any) {
        await recordHarvestMutation.mutateAsync({ data: payload } as any);
        queryClient.invalidateQueries({ queryKey: ['api', 'harvest-logs'] });
        queryClient.invalidateQueries({ queryKey: ['api', 'batches'] });
        queryClient.invalidateQueries({ queryKey: ['api', 'ponds'] });
    }

    return {
        // Data
        harvestLogs,

        // State
        isLoading,
        error,

        // Actions
        refetch,
        recordHarvest,
        isRecording: recordHarvestMutation.isPending,
    };
}
