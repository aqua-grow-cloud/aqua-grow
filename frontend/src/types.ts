export type PondType = 'Earthen' | 'Lined' | 'Tank' | 'Biofloc';
export type PondStatus = 'Active' | 'Inactive' | 'Maintenance';

export interface Pond {
    id: string;
    name: string;
    area: number; // in square meters
    depth: number; // in meters
    type: PondType;
    status: PondStatus;
    currentBatchId?: string | null;
}

export type BatchStatus = 'Active' | 'Harvested' | 'Cancelled';

export interface FishSeed {
    id: string;
    batchId: string;
    date: string; // Stocking date
    type: string; // Species
    quantity: number;
    sizeInches?: number;
    sizeGrams?: number;
}

export interface Batch {
    id: string;
    pondId: string;
    name?: string;
    species: string; // Primary species (compatibility)
    strain?: string;
    stockingDate: string; // Primary stocking date
    estimatedHarvestDate?: string;
    seedSize: number; // Avg size (compatibility)
    initialQuantity: number; // Total quantity (compatibility)
    status: BatchStatus;
    fishSeeds?: FishSeed[];
}

export interface FeedLog {
    id: string;
    batchId: string;
    date: string; // ISO Date string
    feedType: string;
    amount: number; // in kg
    time: string; // HH:mm
}

export interface WaterQuality {
    do: number; // Dissolved Oxygen (mg/L)
    ph: number;
    temperature: number; // Celsius
    ammonia: number; // mg/L
    salinity: number; // ppt
    turbidity?: number;
}

export interface SampleLog {
    id: string;
    batchId: string;
    date: string; // ISO Date string
    sampleCount: number; // Number of pieces
    totalSampleWeight: number; // grams
    averageWeight: number; // grams (calculated)
    averageLength?: number; // cm
    survivalEstimate?: number; // percentage
    waterQuality: WaterQuality;
}

export interface HarvestLog {
    id: string;
    batchId: string;
    pondId?: string; // Multi-pond harvest tracking
    date: string; // ISO Date string
    biomassWeight: number; // Raw KG (Tonne in example)
    count: number; // pieces
    avgWeightGrams: number;
    basicRate: number; // Negotiated price per KG
    finalRate: number; // Computed price per KG
    discountWeight: number; // Free mass (5%)
    actualWeight: number; // Billing mass
    revenue: number; // Total Amount
    commission?: number; // Commission amount
    buyer?: string;
    isFinal?: boolean;
}

export interface Expenditure {
    id: string;
    date: string;
    category: 'Feed' | 'Energy' | 'Labor' | 'Seed' | 'Maintenance' | 'Other' | 'Lease' | 'Advance' | 'NoReturn';
    macroInvestment?: string; // e.g., "ADVANCE", "LEASE", "NO_RETURN"
    subType?: string; // e.g., "Employee_Advance", "LEASE"
    batchId?: string; // Link to a specific batch timeline
    amount: number;
    description: string;
}

export interface Employee {
    id: string;
    name: string;
    role: string;
    salary: number; // Base monthly salary
    advanceTaken: number; // Current outstanding advance
    status: 'Active' | 'Inactive';
    joinedDate: string;
}

export interface SalaryPayment {
    id: string;
    employeeId: string;
    date: string;
    baseSalary: number;
    advanceDeduction: number;
    bonus: number;
    netAmount: number;
    month: string; // e.g., "2023-11"
}

export type UserRole = 'Admin' | 'Manager' | 'Operator';

export interface User {
    id: string;
    name: string;
    role: UserRole;
}
