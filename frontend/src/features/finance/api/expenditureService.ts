import {
    useGetAllExpenditures,
    useLogExpenditure,
    type GetAllExpendituresQueryResult,
    type LogExpenditureMutationResult
} from '../../../api/generated/expenditure-controller/expenditure-controller';
import { customInstance } from '../../../api/axios-instance';
import type { Expenditure } from '../../../api/generated/model';

/**
 * Expenditure Service
 * Wraps Orval-generated hooks with business logic and error handling
 */
export const expenditureService = {
    /**
     * Get all expenditures hook
     */
    useGetAll() {
        return useGetAllExpenditures();
    },

    /**
     * Log expenditure hook
     */
    useCreate() {
        return useLogExpenditure();
    },

    /**
     * Search expenditures with pagination
     */
    search(params: ExpenditureSearchParams) {
        // Import dynamically or assume it's available via the import at top if I add it.
        // Wait, I need to add the import to the top of the file first.
        return customInstance<PageExpenditure>({
            url: '/api/expenditures/search',
            method: 'get',
            params
        });
    }
};

// Type exports for convenience
export type {
    Expenditure,
    GetAllExpendituresQueryResult,
    LogExpenditureMutationResult
};

export interface ExpenditureSearchParams {
    batchId?: string;
    macroInvestment?: string;
    category?: string;
    page?: number;
    size?: number;
    sort?: string;
}

export interface PageExpenditure {
    content?: Expenditure[];
    totalElements?: number;
    totalPages?: number;
    size?: number;
    number?: number;
}
