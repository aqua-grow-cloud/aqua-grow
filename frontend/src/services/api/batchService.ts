import {
    useGetAllBatches,
    useCreateBatch,
    useUpdateBatch,
    useGetBatchesByPond
} from '../../api/manual/hooks';

/**
 * Batch Service
 * Wraps manual hooks with business logic and error handling
 */
export const batchService = {
    /**
     * Get all batches hook
     */
    useGetAll: useGetAllBatches,

    /**
     * Create batch hook
     */
    useCreate: useCreateBatch,

    /**
     * Update batch hook
     */
    useUpdate: useUpdateBatch,

    /**
     * Get batches by pond hook
     */
    useGetByPond: useGetBatchesByPond
};
