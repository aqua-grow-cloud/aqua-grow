import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Pond, Batch, FishSeed, FeedLog, SampleLog, HarvestLog, Expenditure, Employee, SalaryPayment, User, UserRole } from '../types';

export const useAquaStore = defineStore('aqua', () => {
    // --- State ---
    const currentUser = ref<User>({ id: 'u1', name: 'Admin User', role: 'Admin' });

    const ponds = ref<Pond[]>([
        { id: 'p1', name: 'Big Pond', area: 5000, depth: 1.5, type: 'Earthen', status: 'Active', currentBatchId: 'b1' },
        { id: 'p2', name: 'Small Pond', area: 3000, depth: 1.2, type: 'Lined', status: 'Active', currentBatchId: 'b1' },
    ]);

    const batches = ref<Batch[]>([
        {
            id: 'b1',
            pondId: 'p1',
            name: '2024-07-11',
            species: 'Rupchand',
            stockingDate: '2024-07-11',
            estimatedHarvestDate: '2025-01-23',
            seedSize: 0.05,
            initialQuantity: 100000,
            status: 'Active'
        }
    ]);

    const feedLogs = ref<FeedLog[]>([]);
    const sampleLogs = ref<SampleLog[]>([]);
    const harvestLogs = ref<HarvestLog[]>([
        {
            id: 'h1',
            batchId: 'b1',
            date: '2025-01-22',
            biomassWeight: 26878,
            count: 27203,
            avgWeightGrams: 988.05,
            basicRate: 122,
            finalRate: 121.761,
            discountWeight: 1343.9,
            actualWeight: 25534.1,
            revenue: 3109058.97,
            buyer: 'Oceanic Exports',
            isFinal: false
        },
        {
            id: 'h2',
            batchId: 'b1',
            date: '2025-01-23',
            biomassWeight: 12940,
            count: 13549,
            avgWeightGrams: 955.05,
            basicRate: 122,
            finalRate: 121.101,
            discountWeight: 647,
            actualWeight: 12293,
            revenue: 1488695.09,
            buyer: 'Oceanic Exports',
            isFinal: false
        },
        {
            id: 'h3',
            batchId: 'b1',
            date: '2025-01-23',
            biomassWeight: 6984,
            count: 7834,
            avgWeightGrams: 891.49,
            basicRate: 121,
            finalRate: 118.829,
            discountWeight: 349.2,
            actualWeight: 6634.8,
            revenue: 788413.10,
            buyer: 'Fresh Fish Co',
            isFinal: false
        },
        {
            id: 'h4',
            batchId: 'b1',
            date: '2025-01-24',
            biomassWeight: 4326,
            count: 5115,
            avgWeightGrams: 845.74,
            basicRate: 121,
            finalRate: 117.914,
            discountWeight: 216.3,
            actualWeight: 4109.7,
            revenue: 484595.09,
            buyer: 'Fresh Fish Co',
            isFinal: true
        }
    ]);
    const expenditures = ref<Expenditure[]>([
        { id: 'exp1', date: '2024-07-12', category: 'Advance', macroInvestment: 'ADVANCE', subType: 'Employee_Advance', batchId: 'b1', amount: 89000, description: 'Annual Advance Pool' },
        { id: 'exp2', date: '2024-07-15', category: 'Lease', macroInvestment: 'LEASE', subType: 'LEASE', batchId: 'b1', amount: 1624700, description: 'Annual Lease Payment' },
        { id: 'exp3', date: '2024-07-20', category: 'NoReturn', macroInvestment: 'NO_RETURN', subType: '', batchId: 'b1', amount: 1350, description: 'Miscellaneous expense' }
    ]);
    const employees = ref<Employee[]>([
        { id: 'e1', name: 'John Doe', role: 'Operator', salary: 15000, advanceTaken: 0, status: 'Active', joinedDate: '2023-01-15' },
        { id: 'e2', name: 'Jane Smith', role: 'Biologist', salary: 25000, advanceTaken: 2000, status: 'Active', joinedDate: '2023-03-10' },
    ]);
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
        addPond, updatePond, deletePond, addBatch, addFishSeed, addEmployee, updateEmployee, addAdvance, processSalary, logExpenditure, logFeed, logSample, recordHarvest, setUserRole
    };
});
