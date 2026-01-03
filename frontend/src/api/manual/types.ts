export interface Supplier {
    id?: string;
    name: string;
    phone?: string;
    whatsappNumber?: string;
    email?: string;
    address?: string;
    feedTypes?: string[];
    orderTemplate?: string;
    tenantId?: string;
}

export interface FeedStock {
    id?: string;
    feedType: string;
    currentStock: number;
    threshold: number;
    unit: string;
    supplierId?: string;
    lastUpdated?: string;
    lastStockingDate?: string;
    tenantId?: string;
}

export enum OrderStatus {
    PENDING = 'PENDING',
    CONFIRMED = 'CONFIRMED',
    RECEIVED = 'RECEIVED',
    CANCELLED = 'CANCELLED'
}

export enum PaymentStatus {
    UNPAID = 'UNPAID',
    PAID = 'PAID',
    PARTIAL = 'PARTIAL'
}

export interface FeedOrder {
    id?: string;
    feedType: string;
    quantity: number;
    status?: OrderStatus;
    paymentStatus?: PaymentStatus;
    supplierId: string;
    orderDate?: string;
    confirmationDate?: string;
    receivedDate?: string;
    notes?: string;
    tenantId?: string;
}

export interface OrderMessageResponse {
    message: string;
    whatsappLink: string;
    supplierPhone: string;
}

export interface FishSeed {
    id?: string;
    pondId?: string;
    date: string;
    type: string;
    quantity: number;
    sizeInches?: number;
    sizeGrams?: number;
    description?: string;
    batchId?: string;
    status?: string;
}

export interface Batch {
    id?: string;
    pondId: string;
    name?: string;
    stockingDate: string;
    estimatedHarvestDate?: string;
    status: 'Active' | 'Harvested';
    fishSeeds?: FishSeed[];
    tenantId?: string;
}

export interface MonitoringLog {
    id?: string;
    batchId: string;
    date: string;
    doLevel?: number;
    ph?: number;
    temperature?: number;
    ammonia?: number;
    salinity?: number;
    turbidity?: number;
    tenantId?: string;
}

export interface SampleLog {
    id?: string;
    batchId: string;
    fishType?: string;
    date: string;
    sampleCount?: number;
    totalSampleWeight?: number;
    averageWeight?: number;
    averageLength?: number;
    survivalEstimate?: number;
    tenantId?: string;
}
