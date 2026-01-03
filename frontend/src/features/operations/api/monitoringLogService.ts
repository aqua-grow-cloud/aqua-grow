import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query';
import { AXIOS_INSTANCE } from '@/api/axios-instance.ts';
import type { MonitoringLog } from '@/api/manual/types.ts';
import { unref, type MaybeRef } from 'vue';

export const monitoringLogService = {
    useGetAll: () => {
        return useQuery({
            queryKey: ['monitoring-logs'],
            queryFn: async () => {
                const { data } = await AXIOS_INSTANCE.get<MonitoringLog[]>('/api/monitoring-logs');
                return data;
            }
        });
    },

    useSearch: (params: MaybeRef<any>) => {
        return useQuery({
            queryKey: ['monitoring-logs', 'search', params],
            queryFn: async () => {
                const { data } = await AXIOS_INSTANCE.get<any>('/api/monitoring-logs', {
                    params: unref(params)
                });
                return data;
            }
        });
    },

    useLog: () => {
        const queryClient = useQueryClient();
        return useMutation({
            mutationFn: async (payload: { data: MonitoringLog }) => {
                const { data } = await AXIOS_INSTANCE.post<MonitoringLog>('/api/monitoring-logs', payload.data);
                return data;
            },
            onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: ['monitoring-logs'] });
            }
        });
    }
};
