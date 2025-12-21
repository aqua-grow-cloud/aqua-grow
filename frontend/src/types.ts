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

export interface Batch {
    id: string;
    pondId: string;
    species: string; // e.g., 'L. vannamei', 'Tilapia'
    strain?: string;
    stockingDate: string; // ISO Date string
    seedSize: number; // e.g., PL10 or weight in grams
    initialQuantity: number;
    status: BatchStatus;
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
    averageWeight: number; // grams
    averageLength?: number; // cm
    survivalEstimate?: number; // percentage
    waterQuality: WaterQuality;
}

export interface HarvestLog {
    id: string;
    batchId: string;
    date: string; // ISO Date string
    biomassWeight: number; // kg
    count: number; // estimated pieces
    revenue: number;
    buyer?: string;
}

export interface Expenditure {
    id: string;
    date: string;
    category: 'Feed' | 'Energy' | 'Labor' | 'Seed' | 'Maintenance' | 'Other';
    amount: number;
    description: string;
}
