import { expenditureService } from '../services/api';

/**
 * Expenditures Composable
 * Provides reusable expenditure operations with loading/error states
 */
export function useExpenditures() {
// Queries
    const { data: expenditures, isLoading, error, refetch } = expenditureService.useGetAll();

    return {
        // Data
        expenditures,

        // State
        isLoading,
        error,

        // Actions
        refetch,
    };
}
