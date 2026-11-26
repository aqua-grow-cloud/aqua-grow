import {
    useGetAllFishTypes,
    useCreateFishType,
    useUpdateFishType,
    useDeleteFishType,
    type FishType
} from '../../../api/manual/hooks';

/**
 * Fish Type Service
 * Wraps manual hooks for settings feature
 */
export const fishTypeService = {
    /**
     * Get all fish types hook
     */
    useGetAll: useGetAllFishTypes,

    /**
     * Create fish type hook
     */
    useCreate: useCreateFishType,

    /**
     * Update fish type hook
     */
    useUpdate: useUpdateFishType,

    /**
     * Delete fish type hook
     */
    useDelete: useDeleteFishType
};

export type {
    FishType
};
