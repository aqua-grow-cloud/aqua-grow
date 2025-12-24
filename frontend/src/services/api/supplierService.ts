import { useGetAllSuppliers, useCreateSupplier, useUpdateSupplier, useDeleteSupplier, useGetAttachedSupplier } from '../../api/manual/hooks';

export const supplierService = {
    useGetAll: useGetAllSuppliers,
    useGetAttached: useGetAttachedSupplier,
    useCreate: useCreateSupplier,
    useUpdate: useUpdateSupplier,
    useDelete: useDeleteSupplier,
};
