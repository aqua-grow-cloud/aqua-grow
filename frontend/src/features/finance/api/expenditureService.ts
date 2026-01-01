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
        return customInstance<PageExpenditure>({
            url: '/api/expenditures/search',
            method: 'get',
            params
        });
    },

    /**
     * Export expenditures to CSV
     */
    async exportCsv() {
        const response = await customInstance<Blob>({
            url: '/api/expenditures/export',
            method: 'get',
            responseType: 'blob'
        });

        const url = window.URL.createObjectURL(new Blob([response]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'expenditures.csv');
        document.body.appendChild(link);
        link.click();
        link.remove();
    },

    /**
     * Import expenditures from CSV
     */
    async importCsv(file: File) {
        const formData = new FormData();
        formData.append('file', file);

        return customInstance({
            url: '/api/expenditures/import',
            method: 'post',
            data: formData,
            headers: {
                'Content-Type': 'multipart/form-data'
            }
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
