import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query';
import { AXIOS_INSTANCE } from '../axios-instance';
import type { Supplier, FeedStock, FeedOrder, OrderMessageResponse, Batch } from './types';
import type { GetAllBatchesParams } from '../generated/model';
import { computed, unref, type MaybeRef } from 'vue';

// --- Suppliers ---

export const useGetAllSuppliers = () => {
    return useQuery({
        queryKey: ['suppliers'],
        queryFn: async () => {
            const { data } = await AXIOS_INSTANCE.get<Supplier[]>('/api/suppliers');
            return data;
        }
    });
};

export const useGetAttachedSupplier = () => {
    return useQuery({
        queryKey: ['suppliers', 'attached'],
        queryFn: async () => {
            try {
                const { data } = await AXIOS_INSTANCE.get<Supplier>('/api/suppliers/attached');
                return data;
            } catch (error) {
                // If 404 or other error, return null so we don't break the UI
                return null;
            }
        },
        retry: false
    });
};

export const useCreateSupplier = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (supplier: Supplier) => {
            const { data } = await AXIOS_INSTANCE.post<Supplier>('/api/suppliers', supplier);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['suppliers'] });
        }
    });
};

export const useUpdateSupplier = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (payload: { id: string; data: Supplier }) => {
            const { data } = await AXIOS_INSTANCE.put<Supplier>(`/api/suppliers/${payload.id}`, payload.data);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['suppliers'] });
        }
    });
};

export const useDeleteSupplier = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (id: string) => {
            await AXIOS_INSTANCE.delete(`/api/suppliers/${id}`);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['suppliers'] });
        }
    });
};

// --- Feed Stock ---

export const useGetAllFeedStock = () => {
    return useQuery({
        queryKey: ['feed-stock'],
        queryFn: async () => {
            const { data } = await AXIOS_INSTANCE.get<FeedStock[]>('/api/feed-stock');
            return data;
        }
    });
};

export const useGetLowStock = () => {
    return useQuery({
        queryKey: ['feed-stock', 'low'],
        queryFn: async () => {
            const { data } = await AXIOS_INSTANCE.get<FeedStock[]>('/api/feed-stock/low');
            return data;
        }
    });
};

export const useCreateFeedStock = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (feedStock: FeedStock) => {
            const { data } = await AXIOS_INSTANCE.post<FeedStock>('/api/feed-stock', feedStock);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['feed-stock'] });
        }
    });
};

export const useUpdateFeedStock = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (payload: { id: string; data: FeedStock }) => {
            const { data } = await AXIOS_INSTANCE.put<FeedStock>(`/api/feed-stock/${payload.id}`, payload.data);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['feed-stock'] });
        }
    });
};

export const useDeleteFeedStock = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (id: string) => {
            await AXIOS_INSTANCE.delete(`/api/feed-stock/${id}`);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['feed-stock'] });
        }
    });
};

// --- Feed Orders ---

export const useGetAllOrders = () => {
    return useQuery({
        queryKey: ['feed-orders'],
        queryFn: async () => {
            const { data } = await AXIOS_INSTANCE.get<FeedOrder[]>('/api/feed-orders');
            return data;
        }
    });
};

export const useCreateOrder = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (order: FeedOrder) => {
            const { data } = await AXIOS_INSTANCE.post<FeedOrder>('/api/feed-orders', order);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['feed-orders'] });
        }
    });
};

export const useGetOrderMessage = (id: string | undefined, contactPerson?: string, location?: string) => {
    // Only fetch if ID is present
    const enabled = computed(() => !!id);
    return useQuery({
        queryKey: ['feed-orders', id, 'message', contactPerson, location],
        queryFn: async () => {
            if (!id) return null;
            const params = new URLSearchParams();
            if (contactPerson) params.append('contactPerson', contactPerson);
            if (location) params.append('location', location);

            const { data } = await AXIOS_INSTANCE.get<OrderMessageResponse>(`/api/feed-orders/${id}/message?${params.toString()}`);
            return data;
        },
        enabled
    });
};


export const useConfirmOrder = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (id: string) => {
            const { data } = await AXIOS_INSTANCE.put<FeedOrder>(`/api/feed-orders/${id}/confirm`);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['feed-orders'] });
        }
    });
};

export const useReceiveOrder = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (id: string) => {
            const { data } = await AXIOS_INSTANCE.put<FeedOrder>(`/api/feed-orders/${id}/receive`);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['feed-orders'] });
            queryClient.invalidateQueries({ queryKey: ['feed-stock'] }); // Update stock too
        }
    });
};

export const useCancelOrder = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (id: string) => {
            await AXIOS_INSTANCE.delete(`/api/feed-orders/${id}`);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['feed-orders'] });
        }
    });
};

export const useUpdateOrderPaymentStatus = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (payload: { id: string; status: string }) => {
            const { data } = await AXIOS_INSTANCE.put<FeedOrder>(`/api/feed-orders/${payload.id}/payment?status=${payload.status}`);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['feed-orders'] });
        }
    });
};

// --- Batches ---

export const useGetAllBatches = (params?: MaybeRef<GetAllBatchesParams>) => {
    return useQuery({
        queryKey: ['batches', params],
        queryFn: async () => {
            const { data } = await AXIOS_INSTANCE.get<Batch[]>('/api/batches', {
                params: unref(params)
            });
            return data;
        }
    });
};

export const useCreateBatch = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (payload: { data: Batch }) => {
            const { data } = await AXIOS_INSTANCE.post<Batch>('/api/batches', payload.data);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['batches'] });
        }
    });
};

export const useUpdateBatch = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (payload: { id: string; data: Batch }) => {
            const { data } = await AXIOS_INSTANCE.put<Batch>(`/api/batches/${payload.id}`, payload.data);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['batches'] });
        }
    });
};

export const useGetBatchesByPond = (pondId: string) => {
    return useQuery({
        queryKey: ['batches', 'pond', pondId],
        queryFn: async () => {
            if (!pondId) return [];
            const { data } = await AXIOS_INSTANCE.get<Batch[]>(`/api/batches/pond/${pondId}`);
            return data;
        },
        enabled: !!pondId
    });
};

export const useAddFishSeed = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (payload: { batchId: string; seed: any }) => {
            const { data } = await AXIOS_INSTANCE.post<Batch>(`/api/batches/${payload.batchId}/seeds`, payload.seed);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['batches'] });
        }
    });
};

export const useMoveFishSeed = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (payload: { batchId: string; seedId: string; targetPondId: string; quantity?: number }) => {
            const params = new URLSearchParams();
            params.append('targetPondId', payload.targetPondId);
            if (payload.quantity) params.append('quantity', payload.quantity.toString());

            const { data } = await AXIOS_INSTANCE.post<Batch>(`/api/batches/${payload.batchId}/seeds/${payload.seedId}/move?${params.toString()}`);
            return data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['batches'] });
            queryClient.invalidateQueries({ queryKey: ['ponds'] });
        }
    });
};
