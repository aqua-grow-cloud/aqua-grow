import {
	type LogSampleMutationResult,
	type SearchSampleLogsQueryResult,
	useGetSampleLogsByBatch,
	useLogSample,
	useSearchSampleLogs
} from '../../../api/generated/sample-log-controller/sample-log-controller';
import type {SampleLog} from '../../../api/generated/model';
import type {MaybeRef} from 'vue';

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
     * Get all sample logs hook (legacy - now uses search with large page size)
     */
    useGetAll() {
        return useSearchSampleLogs({ pageable: { page: 0, size: 10000 } });
    },

    /**
     * Get sample logs for a specific batch (unpaginated)
     */
    useGetByBatch(batchId: MaybeRef<string>) {
        return useGetSampleLogsByBatch(batchId);
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
