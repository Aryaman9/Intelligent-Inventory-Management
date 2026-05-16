import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import type {
  Inventory,
  InventoryStats,
  AlertsData,
  AddInventoryRequest,
  ApiResponse,
  PaginatedData,
} from '@/lib/types';

export function useStoreInventory(storeId: string, page = 1, lowStockOnly = false) {
  return useQuery({
    queryKey: ['inventory', storeId, page, lowStockOnly],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: String(page),
        limit: '20',
        low_stock_only: String(lowStockOnly),
      });
      const { data } = await api.get<ApiResponse<PaginatedData<Inventory>>>(
        `/inventory/store/${storeId}?${params}`
      );
      return data.data;
    },
    enabled: !!storeId,
  });
}

export function useInventoryItem(id: string) {
  return useQuery({
    queryKey: ['inventory', 'item', id],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<Inventory>>(`/inventory/${id}`);
      return data.data;
    },
    enabled: !!id,
  });
}

export function useAddInventory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (payload: AddInventoryRequest) => {
      const { data } = await api.post<ApiResponse<Inventory>>('/inventory', payload);
      return data.data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['inventory'] }),
  });
}

export function useUpdateInventory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...payload }: Partial<Inventory> & { id: string }) => {
      const { data } = await api.patch<ApiResponse<Inventory>>(`/inventory/${id}`, payload);
      return data.data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['inventory'] }),
  });
}

export function useDeleteInventory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/inventory/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['inventory'] }),
  });
}

export function useInventoryStats(storeId: string) {
  return useQuery({
    queryKey: ['inventory', 'stats', storeId],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<InventoryStats>>(`/inventory/stats/${storeId}`);
      return data.data;
    },
    enabled: !!storeId,
  });
}

export function useAlerts() {
  return useQuery({
    queryKey: ['inventory', 'alerts'],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<AlertsData>>('/inventory/alerts');
      return data.data;
    },
  });
}
