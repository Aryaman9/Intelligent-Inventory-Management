import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Package, TrendingDown, AlertTriangle, CalendarClock } from 'lucide-react';
import { useStore } from '@/hooks/useStores';
import { useInventoryStats } from '@/hooks/useInventory';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { InventoryTable } from '@/components/inventory/InventoryTable';

export function InventoryPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const storeId = id ?? '';

  const { data: store, isLoading: storeLoading } = useStore(storeId);
  const { data: stats, isLoading: statsLoading } = useInventoryStats(storeId);

  if (storeLoading) {
    return (
      <div className="space-y-4">
        <div className="h-8 w-64 bg-zinc-100 rounded animate-pulse" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-24 bg-zinc-100 rounded animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate(`/stores/${storeId}`)}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-semibold">Inventory</h1>
          {store && (
            <p className="text-sm text-muted-foreground">{store.name}</p>
          )}
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-sm font-medium text-muted-foreground">Total Items</CardTitle>
            <Package className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {statsLoading ? (
              <div className="h-7 w-16 bg-zinc-100 rounded animate-pulse" />
            ) : (
              <p className="text-2xl font-bold">{stats?.totalItems ?? 0}</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-sm font-medium text-muted-foreground">Total Value</CardTitle>
            <TrendingDown className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {statsLoading ? (
              <div className="h-7 w-24 bg-zinc-100 rounded animate-pulse" />
            ) : (
              <p className="text-2xl font-bold">
                ₹{(stats?.totalValue ?? 0).toLocaleString('en-IN')}
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-sm font-medium text-muted-foreground">Low Stock</CardTitle>
            <AlertTriangle className="h-4 w-4 text-yellow-500" />
          </CardHeader>
          <CardContent>
            {statsLoading ? (
              <div className="h-7 w-12 bg-zinc-100 rounded animate-pulse" />
            ) : (
              <p className="text-2xl font-bold text-yellow-600">{stats?.lowStockCount ?? 0}</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-sm font-medium text-muted-foreground">Expiring Soon</CardTitle>
            <CalendarClock className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            {statsLoading ? (
              <div className="h-7 w-12 bg-zinc-100 rounded animate-pulse" />
            ) : (
              <p className="text-2xl font-bold text-red-600">{stats?.expiringCount ?? 0}</p>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Inventory Table */}
      <InventoryTable storeId={storeId} />
    </div>
  );
}
