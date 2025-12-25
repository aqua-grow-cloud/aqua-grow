import {
    useGetAllEmployees,
    useCreateEmployee,
    useUpdateEmployee,
    type GetAllEmployeesQueryResult
} from '../../../api/generated/employee-controller/employee-controller';
import type { Employee } from '../../../api/generated/model';

/**
 * Employee Service
 * Wraps Orval-generated hooks with business logic and error handling
 */
export const employeeService = {
    /**
     * Get all employees hook
     */
    useGetAllEmployees() {
        return useGetAllEmployees();
    },

    /**
     * Create employee hook
     */
    useCreateEmployee() {
        return useCreateEmployee();
    },

    /**
     * Update employee hook
     */
    useUpdateEmployee() {
        return useUpdateEmployee();
    },
};

// Type exports for convenience
export type {
    Employee,
    GetAllEmployeesQueryResult
};
