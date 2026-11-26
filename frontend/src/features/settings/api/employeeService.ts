import {type Employee, useGetAllEmployees, useUpdateEmployee} from '../../../api/manual/hooks';

export const employeeService = {
    useGetAll: useGetAllEmployees,
    useUpdate: useUpdateEmployee
};

export type { Employee };
