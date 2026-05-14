import {
    type LogSampleMutationResult,
    type SearchSampleLogsQueryResult,
    useGetSampleLogsByBatch,
    useLogSample,
    useSearchSampleLogs
} from '@/api/generated/sample-log-controller/sample-log-controller.ts';
import {customInstance} from '@/api/axios-instance.ts';
import {useQuery} from '@tanstack/vue-query';
import type {SampleLog} from '@/api/generated/model';
import type {MaybeRef} from 'vue';

// DTO matching DateSpeciesPivotRow on the backend
export interface DateSpeciesPivotRow {
    date: string;                        // ISO date string e.g. "2025-01-12"
    abwBySpecies: Record<string, number>; // fishTypeId → weighted ABW (g)
    countBySpecies: Record<string, number>; // fishTypeId → total fish sampled
}

// DTO matching SamplePivotRow on the backend
export interface SamplePivotEntry {
    date: string;
    sampleCount: number;
    averageWeight: number;
    dayNumber: number;
}

export interface SamplePivotRow {
    batchId: string;
    totalFishSampled: number;
    weightedAvgWeight: number;
    minWeight: number;
    maxWeight: number;
    growthRate: number | null;
    firstSampleDate: string | null;
    lastSampleDate: string | null;
    entries: SamplePivotEntry[];
}

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

    /**
     * Pre-aggregated pivot data grouped by date × species.
     * Each row = one date; columns = one ABW value per fishTypeId.
     */
    useGetPivotByDate() {
        return useQuery({
            queryKey: ['sample-logs', 'pivot', 'by-date'],
            queryFn: async () => {
                return customInstance<DateSpeciesPivotRow[]>({
                    url: '/api/sample-logs/pivot/by-date',
                    method: 'get',
                });
            },
        });
    },

    /**
     * Pre-aggregated pivot data per batch — server does all grouping and calculations.
     * Replaces fetching large raw log pages on the frontend.
     */
    useGetPivot() {
        return useQuery({
            queryKey: ['sample-logs', 'pivot'],
            queryFn: async () => {
                return customInstance<SamplePivotRow[]>({
                    url: '/api/sample-logs/pivot',
                    method: 'get',
                });
            },
        });
    },

    /**
     * Update an existing sample log
     */
    update(id: string, data: SampleLog) {
        return customInstance<SampleLog>({
            url: `/api/sample-logs/${id}`,
            method: 'put',
            data,
        });
    },

    /**
     * Delete a sample log
     */
    delete(id: string) {
        return customInstance<void>({
            url: `/api/sample-logs/${id}`,
            method: 'delete',
        });
    },
};

// Type exports for convenience
export type {
    SampleLog,
    SearchSampleLogsQueryResult,
    LogSampleMutationResult
};

