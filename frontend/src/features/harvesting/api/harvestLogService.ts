import {
    useGetAllHarvestLogs,
    useRecordHarvest,
    type GetAllHarvestLogsQueryResult,
    type RecordHarvestMutationResult
} from '../../../api/generated/harvest-log-controller/harvest-log-controller';
import type { HarvestLog, GetAllHarvestLogsParams } from '../../../api/generated/model';

/**
 * Harvest Log Service
 * Wraps Orval-generated hooks with business logic and error handling
 */
export const harvestLogService = {
    /**
     * Get all harvest logs hook
     */
    useGetAll(params?: any) {
        return useGetAllHarvestLogs(params);
    },

    /**
     * Record harvest hook
     */
    useRecord() {
        return useRecordHarvest();
    },
};

// Type exports for convenience
export type {
    HarvestLog,
    GetAllHarvestLogsParams,
    GetAllHarvestLogsQueryResult,
    RecordHarvestMutationResult
};
