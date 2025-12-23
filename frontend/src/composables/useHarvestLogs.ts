import { harvestLogService } from '../services/api';

/**
 * Harvest Logs Composable
 * Provides reusable harvest log operations with loading/error states
 */
export function useHarvestLogs() {
    // Queries
    const { data: harvestLogs, isLoading, error, refetch } = harvestLogService.useGetAll();

    return {
        // Data
        harvestLogs,

        // State
        isLoading,
        error,

        // Actions
        refetch,
    };
}
