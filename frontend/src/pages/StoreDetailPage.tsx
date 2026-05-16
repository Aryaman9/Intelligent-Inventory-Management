import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  MapPin,
  Phone,
  Mail,
  Building2,
  ShoppingCart,
  PackagePlus,
  BarChart3,
  Boxes,
  AlertTriangle,
  Package,
  TrendingUp,
  DollarSign,
} from 'lucide-react';
import { useStore } from '@/hooks/useStores';
import { useInventoryStats } from '@/hooks/useInventory';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { InventoryTable } from '@/components/inventory/InventoryTable';
import { TransactionTable } from '@/components/transactions/TransactionTable';

export function StoreDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: store, isLoading } = useStore(id ?? '');
  const { data: inventoryStats } = useInventoryStats(id ?? '');

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-8 w-48 bg-zinc-100 rounded animate-pulse" />
        <div className="h-48 bg-zinc-100 rounded animate-pulse" />
      </div>
    );
  }

  if (!store) {
    return (
      <div className="text-center py-16 text-muted-foreground">
        <p>Store not found.</p>
        <Button variant="link" onClick={() => navigate('/stores')}>
          Back to stores
        </Button>
      </div>
    );
  }

  const statsCards = [
    {
      label: 'Total Items',
      value: inventoryStats?.totalItems ?? '—',
      icon: Package,
      color: 'text-blue-600',
    },
    {
      label: 'Total Value',
      value: inventoryStats
        ? `₹${inventoryStats.totalValue.toLocaleString('en-IN')}`
        : '—',
      icon: DollarSign,
      color: 'text-green-600',
    },
    {
      label: 'Low Stock',
      value: inventoryStats?.lowStockCount ?? '—',
      icon: AlertTriangle,
      color: 'text-yellow-600',
    },
    {
      label: 'Expiring Soon',
      value: inventoryStats?.expiringCount ?? '—',
      icon: TrendingUp,
      color: 'text-red-600',
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate('/stores')}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-semibold">{store.name}</h1>
          <p className="text-sm text-muted-foreground capitalize">{store.type.toLowerCase()} store</p>
        </div>
        <Badge variant={store.isActive ? 'default' : 'secondary'} className="ml-auto">
          {store.isActive ? 'Active' : 'Inactive'}
        </Badge>
      </div>

      <div className="flex flex-wrap gap-2">
        <Button onClick={() => navigate(`/stores/${id}/sale`)}>
          <ShoppingCart className="h-4 w-4 mr-2" />
          Record Sale
        </Button>
        <Button variant="outline" onClick={() => navigate(`/stores/${id}/purchase`)}>
          <PackagePlus className="h-4 w-4 mr-2" />
          Restock
        </Button>
        <Button variant="outline" onClick={() => navigate(`/stores/${id}/analytics`)}>
          <BarChart3 className="h-4 w-4 mr-2" />
          Analytics
        </Button>
      </div>

      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="inventory">
            <Boxes className="h-4 w-4 mr-1" />
            Inventory
          </TabsTrigger>
          <TabsTrigger value="transactions">Transactions</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4 mt-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Store Information</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div className="flex items-start gap-2">
                <MapPin className="h-4 w-4 mt-0.5 text-muted-foreground shrink-0" />
                <div>
                  <p className="font-medium">Address</p>
                  <p className="text-muted-foreground">
                    {store.address}, {store.city}, {store.state} — {store.pincode}
                  </p>
                </div>
              </div>
              {store.phone && (
                <div className="flex items-start gap-2">
                  <Phone className="h-4 w-4 mt-0.5 text-muted-foreground shrink-0" />
                  <div>
                    <p className="font-medium">Phone</p>
                    <p className="text-muted-foreground">{store.phone}</p>
                  </div>
                </div>
              )}
              {store.email && (
                <div className="flex items-start gap-2">
                  <Mail className="h-4 w-4 mt-0.5 text-muted-foreground shrink-0" />
                  <div>
                    <p className="font-medium">Email</p>
                    <p className="text-muted-foreground">{store.email}</p>
                  </div>
                </div>
              )}
              {store.gstin && (
                <div className="flex items-start gap-2">
                  <Building2 className="h-4 w-4 mt-0.5 text-muted-foreground shrink-0" />
                  <div>
                    <p className="font-medium">GSTIN</p>
                    <p className="text-muted-foreground">{store.gstin}</p>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {statsCards.map(({ label, value, icon: Icon, color }) => (
              <Card key={label}>
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-xs font-medium text-muted-foreground">{label}</CardTitle>
                  <Icon className={`h-4 w-4 ${color}`} />
                </CardHeader>
                <CardContent>
                  <p className="text-2xl font-bold">{value}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="inventory" className="mt-4">
          <InventoryTable storeId={id ?? ''} />
        </TabsContent>

        <TabsContent value="transactions" className="mt-4">
          <TransactionTable storeId={id ?? ''} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
