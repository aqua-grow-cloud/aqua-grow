import { expenditureService } from '../api/expenditureService';
import { useQueryClient } from '@tanstack/vue-query';

/**
 * Expenditures Composable
 * Provides reusable expenditure operations with loading/error states
 */
export function useExpenditures() {
    const queryClient = useQueryClient();

    // Queries
    const { data: expenditures, isLoading, error, refetch } = expenditureService.useGetAll();

    // Mutations
    const createExpenditure = expenditureService.useCreate();

    // Actions
    async function create(payload: any) {
        await createExpenditure.mutateAsync({ data: payload } as any);
        queryClient.invalidateQueries({ queryKey: ['api', 'expenditures'] });
    }

    return {
        // Data
        expenditures,

        // State
        isLoading,
        error,

        // Actions
        create,
        refetch,
    };
}
