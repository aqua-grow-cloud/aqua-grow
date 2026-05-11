import {
	useAddFishSeed,
	useCreateBatch,
	useGetAllBatches,
	useGetBatchesByPond,
	useLogMortality,
	useMoveFishSeed,
	useUpdateBatch
} from '../../../api/manual/hooks';
import type {Batch, FishSeed} from '../../../api/generated/model';

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
    useGetByPond: useGetBatchesByPond,

    /**
     * Add fish seed to batch hook
     */
    useAddSeed: useAddFishSeed,

    /**
     * Move fish seed between ponds hook
     */
    useMoveSeed: useMoveFishSeed,

    /**
     * Log mortality event for a fish seed hook
     */
    useLogMortality
};

export type {
    Batch,
    FishSeed
};
