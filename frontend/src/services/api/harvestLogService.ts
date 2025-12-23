import {
    useGetAllHarvestLogs,
    type GetAllHarvestLogsQueryResult
} from '../../api/generated/harvest-log-controller/harvest-log-controller';
import type { HarvestLog } from '../../api/generated/model';

/**
 * Harvest Log Service
 * Wraps Orval-generated hooks with business logic and error handling
 */
export const harvestLogService = {
    /**
     * Get all harvest logs hook
     */
    useGetAll() {
        return useGetAllHarvestLogs();
    },
};

// Type exports for convenience
export type {
    HarvestLog,
    GetAllHarvestLogsQueryResult
};
