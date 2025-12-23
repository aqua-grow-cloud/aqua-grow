import {
    useGetAllBatches,
    useCreateBatch,
    useUpdateBatch,
    type GetAllBatchesQueryResult,
    type CreateBatchMutationResult,
    type UpdateBatchMutationResult
} from '../../api/generated/batch-controller/batch-controller';
import type { Batch } from '../../api/generated/model';

/**
 * Batch Service
 * Wraps Orval-generated hooks with business logic and error handling
 */
export const batchService = {
    /**
     * Get all batches hook
     */
    useGetAll() {
        return useGetAllBatches();
    },

    /**
     * Create batch hook
     */
    useCreate() {
        return useCreateBatch();
    },

    /**
     * Update batch hook
     */
    useUpdate() {
        return useUpdateBatch();
    },
};

// Type exports for convenience
export type {
    Batch,
    GetAllBatchesQueryResult,
    CreateBatchMutationResult,
    UpdateBatchMutationResult
};
