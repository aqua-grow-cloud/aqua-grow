import {
	type GetAllHarvestLogsQueryResult,
	type RecordHarvestMutationResult,
	useGetAllHarvestLogs,
	useRecordHarvest
} from '../../../api/generated/harvest-log-controller/harvest-log-controller';
import {customInstance} from '../../../api/axios-instance';
import type {GetAllHarvestLogsParams, HarvestLog} from '../../../api/generated/model';

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

    /**
     * Update an existing harvest log
     */
    update(id: string, data: HarvestLog) {
        return customInstance<HarvestLog>({
            url: `/api/harvest-logs/${id}`,
            method: 'put',
            data,
        });
    },

    /**
     * Delete a harvest log
     */
    delete(id: string) {
        return customInstance<void>({
            url: `/api/harvest-logs/${id}`,
            method: 'delete',
        });
    },
};

// Type exports for convenience
export type {
    HarvestLog,
    GetAllHarvestLogsParams,
    GetAllHarvestLogsQueryResult,
    RecordHarvestMutationResult
};

