import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import type { Product, ApiResponse, PaginatedData } from '@/lib/types';

export function useProducts(page = 1, search = '', category = '') {
  return useQuery({
    queryKey: ['products', page, search, category],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), limit: '12' });
      if (search) params.set('search', search);
      if (category) params.set('category', category);
      const { data } = await api.get<ApiResponse<PaginatedData<Product>>>(`/products?${params}`);
      return data.data;
    },
  });
}

export function useProduct(id: string) {
  return useQuery({
    queryKey: ['products', id],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<Product>>(`/products/${id}`);
      return data.data;
    },
    enabled: !!id,
  });
}

export function useCategories() {
  return useQuery({
    queryKey: ['products', 'categories'],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<string[]>>('/products/categories');
      return data.data;
    },
  });
}

export function useProductByBarcode(barcode: string) {
  return useQuery({
    queryKey: ['products', 'barcode', barcode],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<Product>>(`/products/barcode/${barcode}`);
      return data.data;
    },
    enabled: !!barcode,
  });
}

export function useCreateProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (payload: Partial<Product>) => {
      const { data } = await api.post<ApiResponse<Product>>('/products', payload);
      return data.data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }),
  });
}

export function useUpdateProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...payload }: Partial<Product> & { id: string }) => {
      const { data } = await api.put<ApiResponse<Product>>(`/products/${id}`, payload);
      return data.data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }),
  });
}

export function useDeleteProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/products/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }),
  });
}
