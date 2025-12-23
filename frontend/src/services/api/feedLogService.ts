import {
    useGetAllFeedLogs,
    type GetAllFeedLogsQueryResult
} from '../../api/generated/feed-log-controller/feed-log-controller';
import type { FeedLog } from '../../api/generated/model';

/**
 * Feed Log Service
 * Wraps Orval-generated hooks with business logic and error handling
 */
export const feedLogService = {
    /**
     * Get all feed logs hook
     */
    useGetAll() {
        return useGetAllFeedLogs();
    },
};

// Type exports for convenience
export type {
    FeedLog,
    GetAllFeedLogsQueryResult
};
