import { ref } from 'vue';
import { useQueryClient } from '@tanstack/vue-query';
import { employeeService } from '../api/employeeService'; // Assuming I'll create this or use generated
import { expenditureService } from '../../finance/api/expenditureService';
import type { Employee, SalaryPayment } from '../../../types';

/**
 * Payroll Composable
 * Manages employees and salary processing
 * NOTE: For now, some logic remains frontend-only or maps to Expenditures
 */
export function usePayroll() {
    const queryClient = useQueryClient();

    // Use generated hooks if available, or fallback to mock logic via store/local refs if needed
    // For now, let's assume we use employee-controller for employees
    const {
        useGetAllEmployees,
        useCreateEmployee,
        useUpdateEmployee
    } = employeeService;

    const { data: employees, isLoading, error } = useGetAllEmployees();
    const createEmpMutation = useCreateEmployee();
    const updateEmpMutation = useUpdateEmployee();
    const createExpMutation = expenditureService.useCreate();

    // Actions
    async function addEmployee(emp: any) {
        await createEmpMutation.mutateAsync({ data: emp } as any);
        queryClient.invalidateQueries({ queryKey: ['api', 'employees'] });
    }

    async function updateEmployee(id: string, updates: any) {
        await updateEmpMutation.mutateAsync({ id, data: updates } as any);
        queryClient.invalidateQueries({ queryKey: ['api', 'employees'] });
    }

    async function addAdvance(empId: string, amount: number) {
        // This is a complex action that updates employee (local/mock) and logs expenditure
        // In a real app, this would be one or two API calls
        // For now, we'll log it as a 'Advance' expenditure
        await createExpMutation.mutateAsync({
            data: {
                date: new Date().toISOString().split('T')[0],
                category: 'Advance',
                macroInvestment: 'ADVANCE',
                subType: 'Employee_Advance',
                amount: amount,
                description: `Salary Advance for emp ID: ${empId}`
            }
        } as any);
        queryClient.invalidateQueries({ queryKey: ['api', 'expenditures'] });
    }

    async function processSalary(payment: any) {
        // Log expenditure for salary
        await createExpMutation.mutateAsync({
            data: {
                date: payment.date,
                category: 'Labor',
                macroInvestment: 'NO_RETURN',
                amount: payment.netAmount,
                description: `Salary Payment (${payment.month})`
            }
        } as any);
        queryClient.invalidateQueries({ queryKey: ['api', 'expenditures'] });
    }

    return {
        employees,
        isLoading,
        error,
        addEmployee,
        updateEmployee,
        addAdvance,
        processSalary,
        isProcessing: createExpMutation.isPending || createEmpMutation.isPending
    }
}
