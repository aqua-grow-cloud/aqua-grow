import {
    type GetAllFeedLogsQueryResult,
    type LogFeedMutationResult,
    useGetAllFeedLogs,
    useLogFeed
} from '@/api/generated/feed-log-controller/feed-log-controller.ts';
import {useDeleteFeedLog, useUpdateFeedLog} from '@/api/manual/hooks.ts';
import type {FeedLog} from '@/api/generated/model';

/**
 * Feed Log Service
 * Wraps Orval-generated hooks with business logic and error handling
 */
export const feedLogService = {
    useGetAll() {
        return useGetAllFeedLogs();
    },

    useLog() {
        return useLogFeed();
    },

    useUpdate() {
        return useUpdateFeedLog();
    },

    useDelete() {
        return useDeleteFeedLog();
    },
};

// Type exports for convenience
export type {
    FeedLog,
    GetAllFeedLogsQueryResult,
    LogFeedMutationResult
};
