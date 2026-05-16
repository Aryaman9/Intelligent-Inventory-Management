import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/lib/auth';
import { Toaster } from '@/components/ui/toaster';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { AppLayout } from '@/components/layout/AppLayout';
import { LoginPage } from '@/pages/LoginPage';
import { RegisterPage } from '@/pages/RegisterPage';
import { DashboardPage } from '@/pages/DashboardPage';
import { StoresPage } from '@/pages/StoresPage';
import { StoreDetailPage } from '@/pages/StoreDetailPage';
import { ProductsPage } from '@/pages/ProductsPage';
import { InventoryPage } from '@/pages/InventoryPage';
import { SalePage } from '@/pages/SalePage';
import { PurchasePage } from '@/pages/PurchasePage';
import { TransactionsPage } from '@/pages/TransactionsPage';
import { AlertsPage } from '@/pages/AlertsPage';
import { AnalyticsPage } from '@/pages/AnalyticsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
});

export default function App() {
  return (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/stores" element={<StoresPage />} />
                <Route path="/stores/:id" element={<StoreDetailPage />} />
                <Route path="/products" element={<ProductsPage />} />
                <Route path="/stores/:id/inventory" element={<InventoryPage />} />
                <Route path="/stores/:id/sale" element={<SalePage />} />
                <Route path="/stores/:id/purchase" element={<PurchasePage />} />
                <Route path="/stores/:id/transactions" element={<TransactionsPage />} />
                <Route path="/alerts" element={<AlertsPage />} />
                <Route path="/stores/:id/analytics" element={<AnalyticsPage />} />
              </Route>
            </Route>
          </Routes>
          <Toaster />
        </AuthProvider>
      </QueryClientProvider>
    </BrowserRouter>
  );
}
