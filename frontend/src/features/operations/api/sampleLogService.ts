import {
    useGetAllSampleLogs,
    useLogSample,
    type GetAllSampleLogsQueryResult,
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
        return useQuery({
            queryKey: ['sample-logs', 'search', params],
            queryFn: async () => {
                const { data } = await axios.get('/api/sample-logs', { params: params.value });
                return data;
            }
        });
    },

    /**
     * Get all sample logs hook (legacy/simple)
     */
    useGetAll() {
        return useGetAllSampleLogs();
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
    GetAllSampleLogsQueryResult,
    LogSampleMutationResult
};
