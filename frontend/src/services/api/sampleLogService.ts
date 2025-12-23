import {
    useGetAllSampleLogs,
    type GetAllSampleLogsQueryResult
} from '../../api/generated/sample-log-controller/sample-log-controller';
import type { SampleLog } from '../../api/generated/model';

/**
 * Sample Log Service
 * Wraps Orval-generated hooks with business logic and error handling
 */
export const sampleLogService = {
    /**
     * Get all sample logs hook
     */
    useGetAll() {
        return useGetAllSampleLogs();
    },
};

// Type exports for convenience
export type {
    SampleLog,
    GetAllSampleLogsQueryResult
};
