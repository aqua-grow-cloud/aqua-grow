import {
    useGetAllPonds,
    useCreatePond,
    useUpdatePond,
    useDeletePond,
    type GetAllPondsQueryResult,
    type CreatePondMutationResult,
    type UpdatePondMutationResult,
    type DeletePondMutationResult
} from '../../../api/generated/pond-controller/pond-controller';
import type { Pond } from '../../../api/generated/model';

/**
 * Pond Service
 * Wraps Orval-generated hooks with business logic and error handling
 */
export const pondService = {
    /**
     * Get all ponds hook
     */
    useGetAll() {
        return useGetAllPonds();
    },

    /**
     * Create pond hook
     */
    useCreate() {
        return useCreatePond();
    },

    /**
     * Update pond hook
     */
    useUpdate() {
        return useUpdatePond();
    },

    /**
     * Delete pond hook
     */
    useDelete() {
        return useDeletePond();
    },
};

// Type exports for convenience
export type {
    Pond,
    GetAllPondsQueryResult,
    CreatePondMutationResult,
    UpdatePondMutationResult,
    DeletePondMutationResult
};
