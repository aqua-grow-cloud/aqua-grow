import {
    useSearchSampleLogs,
    useLogSample,
    type SearchSampleLogsQueryResult,
    type LogSampleMutationResult
} from '../../../api/generated/sample-log-controller/sample-log-controller';
import type { SampleLog } from '../../../api/generated/model';

import { useQuery } from '@tanstack/vue-query';
import { AXIOS_INSTANCE as axios } from '../../../api/axios-instance';

/**
 * Sample Log Service
 * Wraps Orval-generated hooks with business logic and error handling
 */
export const sampleLogService = {
    /**
     * Search sample logs hook
     */
    useSearch(params: any) {
        return useSearchSampleLogs(params);
    },

    /**
     * Get all sample logs hook (legacy - now uses search with default pagination)
     */
    useGetAll() {
        return useSearchSampleLogs({ pageable: { page: 0, size: 1000 } });
    },

    /**
     * Log sample hook
     */
    useLog() {
        return useLogSample();
    },
};

// Type exports for convenience
export type {
    SampleLog,
    SearchSampleLogsQueryResult,
    LogSampleMutationResult
};
