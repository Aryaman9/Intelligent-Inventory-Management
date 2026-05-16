import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import type {
  ApiResponse,
  PaginatedData,
  Transaction,
  SaleRequest,
  SaleResponse,
  PurchaseRequest,
  TransactionStats,
} from '@/lib/types';

type TransactionType = 'ALL' | 'SALE' | 'PURCHASE' | 'RETURN' | 'ADJUSTMENT';

export function useStoreTransactions(
  storeId: string,
  page = 1,
  type: TransactionType = 'ALL',
  startDate = '',
  endDate = ''
) {
  return useQuery({
    queryKey: ['transactions', storeId, page, type, startDate, endDate],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page) });
      if (type !== 'ALL') params.set('type', type);
      if (startDate) params.set('start_date', startDate);
      if (endDate) params.set('end_date', endDate);
      const { data } = await api.get<ApiResponse<PaginatedData<Transaction>>>(
        `/transactions/store/${storeId}?${params}`
      );
      return data.data;
    },
    enabled: !!storeId,
  });
}

export function useTransaction(id: string) {
  return useQuery({
    queryKey: ['transactions', 'item', id],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<Transaction>>(`/transactions/${id}`);
      return data.data;
    },
    enabled: !!id,
  });
}

interface PurchaseSuccessData {
  transaction: {
    id: string;
    type: string;
    quantity: string;
    totalAmount: string;
  };
  inventoryQuantity: string;
  productName: string;
}

export function useRecordSale() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      data,
      idempotencyKey,
    }: {
      data: SaleRequest;
      idempotencyKey: string;
    }) => {
      const response = await api.post<ApiResponse<SaleResponse>>(
        '/transactions/sale',
        data,
        { headers: { 'Idempotency-Key': idempotencyKey } }
      );
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
  });
}

export function useRecordPurchase() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      data,
      idempotencyKey,
    }: {
      data: PurchaseRequest;
      idempotencyKey: string;
    }) => {
      const response = await api.post<ApiResponse<PurchaseSuccessData>>(
        '/transactions/purchase',
        data,
        { headers: { 'Idempotency-Key': idempotencyKey } }
      );
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
  });
}

export function useTransactionStats(storeId: string, startDate = '', endDate = '') {
  return useQuery({
    queryKey: ['transactions', 'stats', storeId, startDate, endDate],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (startDate) params.set('start_date', startDate);
      if (endDate) params.set('end_date', endDate);
      const query = params.toString() ? `?${params}` : '';
      const { data } = await api.get<ApiResponse<TransactionStats>>(
        `/transactions/stats/${storeId}${query}`
      );
      return data.data;
    },
    enabled: !!storeId,
  });
}
