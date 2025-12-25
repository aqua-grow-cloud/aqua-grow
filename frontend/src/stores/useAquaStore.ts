import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Pond, Batch, FishSeed, FeedLog, SampleLog, HarvestLog, Expenditure, Employee, SalaryPayment, User, UserRole } from '../types';

export const useAquaStore = defineStore('aqua', () => {
    // --- State ---
    const currentUser = ref<User>({ id: 'u1', name: 'Admin User', role: 'Admin' });

    // Global Feedback
    const globalError = ref<string | null>(null);
    const globalErrorType = ref<string>('error'); // error, warning, info
    const isGlobalErrorVisible = ref(false);

    function notify(message: string, type: 'error' | 'warning' | 'info' = 'error') {
        globalError.value = message;
        globalErrorType.value = type;
        isGlobalErrorVisible.value = true;
    }

    function clearNotification() {
        isGlobalErrorVisible.value = false;
        setTimeout(() => {
            globalError.value = null;
        }, 300);
    }

    const ponds = ref<Pond[]>([]);

    const batches = ref<Batch[]>([]);

    const feedLogs = ref<FeedLog[]>([]);
    const sampleLogs = ref<SampleLog[]>([]);
    const harvestLogs = ref<HarvestLog[]>([]);
    const expenditures = ref<Expenditure[]>([]);
    const employees = ref<Employee[]>([]);
    const salaryPayments = ref<SalaryPayment[]>([]);

    // --- Getters ---
    const activePonds = computed(() => ponds.value.filter(p => p.status === 'Active'));
    const getPondById = (id: string) => ponds.value.find(p => p.id === id);
    const getBatchById = (id: string) => batches.value.find(b => b.id === id);
    const getBatchesByPondId = (pondId: string) => batches.value.filter(b => b.pondId === pondId);
    const getEmployeeById = (id: string) => employees.value.find(e => e.id === id);

    const isAdmin = computed(() => currentUser.value.role === 'Admin');
    const isManager = computed(() => currentUser.value.role === 'Manager' || currentUser.value.role === 'Admin');
    const isOperator = computed(() => currentUser.value.role === 'Operator' || currentUser.value.role === 'Manager' || currentUser.value.role === 'Admin');

    function setUserRole(role: UserRole) {
        currentUser.value.role = role;
    }

    // --- Actions ---
    function addPond(pond: Omit<Pond, 'id'>) {
        const newId = `p${Date.now()}`;
        ponds.value.push({ ...pond, id: newId });
    }

    function updatePond(id: string, updates: Partial<Pond>) {
        const index = ponds.value.findIndex(p => p.id === id);
        if (index !== -1) {
            ponds.value[index] = { ...ponds.value[index], ...updates } as Pond;
        }
    }

    function deletePond(id: string) {
        ponds.value = ponds.value.filter(p => p.id !== id);
    }

    function addBatch(batch: Omit<Batch, 'id'>) {
        const newId = `b${Date.now()}`;
        const newBatch = { ...batch, id: newId };
        if (newBatch.fishSeeds) {
            newBatch.fishSeeds = newBatch.fishSeeds.map((seed, index) => ({
                ...seed,
                id: seed.id || `fs${Date.now()}-${index}`,
                batchId: newId
            }));
        }
        batches.value.push(newBatch);
        if (batch.pondId) {
            updatePond(batch.pondId, { currentBatchId: newId, status: 'Active' });
        }
    }

    function addFishSeed(batchId: string, seed: FishSeed) {
        const batch = getBatchById(batchId);
        if (batch) {
            if (!batch.fishSeeds) {
                batch.fishSeeds = [];
            }
            const newSeed = { ...seed, id: seed.id || `fs${Date.now()}`, batchId };
            batch.fishSeeds.push(newSeed);
        }
    }

    function addEmployee(emp: Omit<Employee, 'id'>) {
        employees.value.push({ ...emp, id: `e${Date.now()}` });
    }

    function updateEmployee(id: string, updates: Partial<Employee>) {
        const index = employees.value.findIndex(e => e.id === id);
        if (index !== -1) {
            employees.value[index] = { ...employees.value[index], ...updates } as Employee;
        }
    }

    function addAdvance(empId: string, amount: number) {
        const emp = getEmployeeById(empId);
        if (emp) {
            emp.advanceTaken += amount;
            logExpenditure({
                date: new Date().toISOString().split('T')[0] as string,
                category: 'Advance',
                macroInvestment: 'ADVANCE',
                subType: 'Employee_Advance',
                amount: amount,
                description: `Salary Advance: ${emp.name}`
            });
        }
    }

    function processSalary(payment: Omit<SalaryPayment, 'id'>) {
        const newId = `pay${Date.now()}`;
        salaryPayments.value.push({ ...payment, id: newId });
        const emp = getEmployeeById(payment.employeeId);
        if (emp) {
            emp.advanceTaken -= payment.advanceDeduction;
        }
        logExpenditure({
            date: payment.date,
            category: 'Labor',
            amount: payment.netAmount,
            description: `Salary Payment (${payment.month}): ${emp?.name || 'Unknown'}`
        });
    }

    function logExpenditure(exp: Omit<Expenditure, 'id'>) {
        expenditures.value.push({ ...exp, id: `exp${Date.now()}` });
    }

    function logFeed(log: Omit<FeedLog, 'id'>) {
        feedLogs.value.push({ ...log, id: `f${Date.now()}` });
    }

    function logSample(log: Omit<SampleLog, 'id'>) {
        sampleLogs.value.push({ ...log, id: `s${Date.now()}` });
    }

    function recordHarvest(log: Omit<HarvestLog, 'id'>, isFinal: boolean = true) {
        harvestLogs.value.push({ ...log, id: `h${Date.now()}` });
        if (isFinal) {
            const batch = getBatchById(log.batchId);
            if (batch) {
                batch.status = 'Harvested';
                updatePond(batch.pondId, { currentBatchId: null, status: 'Inactive' });
            }
        }
    }

    return {
        ponds, batches, feedLogs, sampleLogs, harvestLogs, expenditures, employees, salaryPayments, currentUser,
        activePonds, getPondById, getBatchById, getBatchesByPondId, getEmployeeById, isAdmin, isManager, isOperator,
        addPond, updatePond, deletePond, addBatch, addFishSeed, addEmployee, updateEmployee, addAdvance, processSalary, logExpenditure, logFeed, logSample, recordHarvest, setUserRole,
        // Notification
        globalError, globalErrorType, isGlobalErrorVisible, notify, clearNotification
    };
});
