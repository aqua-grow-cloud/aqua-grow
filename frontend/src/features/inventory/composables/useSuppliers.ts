import { supplierService } from '../api/supplierService';

/**
 * Suppliers Composable
 * Provides reusable supplier operations
 */
export function useSuppliers() {
    const { data: suppliers, isLoading, error } = supplierService.useGetAll();

    return {
        suppliers,
        isLoading,
        error
    };
}
