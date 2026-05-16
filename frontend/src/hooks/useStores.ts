import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import type { Store, ApiResponse, PaginatedData, StoreStats } from '@/lib/types';

export function useStores(page = 1, search = '') {
  return useQuery({
    queryKey: ['stores', page, search],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), limit: '10' });
      if (search) params.set('search', search);
      const { data } = await api.get<ApiResponse<PaginatedData<Store>>>(`/stores?${params}`);
      return data.data;
    },
  });
}

export function useStore(id: string) {
  return useQuery({
    queryKey: ['stores', id],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<Store>>(`/stores/${id}`);
      return data.data;
    },
    enabled: !!id,
  });
}

export function useStoreStats() {
  return useQuery({
    queryKey: ['stores', 'stats'],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<StoreStats>>('/stores/stats');
      return data.data;
    },
  });
}

export function useCreateStore() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (payload: Partial<Store>) => {
      const { data } = await api.post<ApiResponse<Store>>('/stores', payload);
      return data.data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['stores'] }),
  });
}

export function useUpdateStore() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...payload }: Partial<Store> & { id: string }) => {
      const { data } = await api.put<ApiResponse<Store>>(`/stores/${id}`, payload);
      return data.data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['stores'] }),
  });
}

export function useDeleteStore() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/stores/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['stores'] }),
  });
}
