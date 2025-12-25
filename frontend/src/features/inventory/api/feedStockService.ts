import {
    useGetAllFeedStock,
    useGetLowStock,
    useCreateFeedStock,
    useUpdateFeedStock,
    useDeleteFeedStock
} from '../../../api/manual/hooks';

export const feedStockService = {
    useGetAll: useGetAllFeedStock,
    useGetLowStock: useGetLowStock,
    useCreate: useCreateFeedStock,
    useUpdate: useUpdateFeedStock,
    useDelete: useDeleteFeedStock,
};
