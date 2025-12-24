export interface Supplier {
    id?: string;
    name: string;
    phone?: string;
    whatsappNumber?: string;
    email?: string;
    address?: string;
    feedTypes?: string[];
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
    tenantId?: string;
}

export enum OrderStatus {
    PENDING = 'PENDING',
    CONFIRMED = 'CONFIRMED',
    RECEIVED = 'RECEIVED',
    CANCELLED = 'CANCELLED'
}

export interface FeedOrder {
    id?: string;
    feedType: string;
    quantity: number;
    status?: OrderStatus;
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
