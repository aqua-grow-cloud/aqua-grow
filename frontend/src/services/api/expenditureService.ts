import {
    useGetAllExpenditures,
    type GetAllExpendituresQueryResult
} from '../../api/generated/expenditure-controller/expenditure-controller';
import type { Expenditure } from '../../api/generated/model';

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
};

// Type exports for convenience
export type {
    Expenditure,
    GetAllExpendituresQueryResult
};
