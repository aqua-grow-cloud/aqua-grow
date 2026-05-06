import {
    type GetAllExpendituresQueryResult,
    type LogExpenditureMutationResult,
    useGetAllExpenditures,
    useLogExpenditure
} from '@/api/generated/expenditure-controller/expenditure-controller.ts';
import {customInstance} from '@/api/axios-instance.ts';
import type {Expenditure} from '@/api/generated/model';

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
     * Get distinct macroInvestment values used by this tenant
     */
    getMacroInvestments() {
        return customInstance<string[]>({
            url: '/api/expenditures/macro-investments',
            method: 'get'
        });
    },

    /**
     * Update an existing expenditure
     */
    update(id: string, data: Expenditure) {
        return customInstance<Expenditure>({
            url: `/api/expenditures/${id}`,
            method: 'put',
            data
        });
    },

    /**
     * Delete an expenditure
     */
    delete(id: string) {
        return customInstance<void>({
            url: `/api/expenditures/${id}`,
            method: 'delete'
        });
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
    subType?: string;
    startDate?: string;
    endDate?: string;
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
