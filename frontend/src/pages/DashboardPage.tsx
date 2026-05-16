import { useNavigate } from 'react-router-dom';
import { Plus, Store, Package, TrendingUp, AlertTriangle, Clock, ShoppingCart } from 'lucide-react';
import { format } from 'date-fns';
import { useAuth } from '@/lib/auth';
import { useStores, useStoreStats } from '@/hooks/useStores';
import { useProducts } from '@/hooks/useProducts';
import { useAlerts } from '@/hooks/useInventory';
import { useStoreTransactions } from '@/hooks/useTransactions';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

export function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { data: storesData } = useStores(1, '');
  const { data: stats } = useStoreStats();
  const { data: productsData } = useProducts(1, '', '');
  const { data: alertsData } = useAlerts();

  const firstStoreId = storesData?.items[0]?.id;
  const { data: recentTxns } = useStoreTransactions(firstStoreId ?? '', 1, 'ALL', '', '');

  const firstName = user?.fullName?.split(' ')[0] ?? 'there';
  const lowStockCount = alertsData?.summary.lowStockCount ?? 0;
  const expiringSoonCount = alertsData?.summary.expiringSoonCount ?? 0;
  const totalAlerts = lowStockCount + expiringSoonCount;

  const statCards = [
    {
      title: 'Total Stores',
      value: storesData?.pagination.total ?? 0,
      icon: Store,
      color: 'text-blue-600',
    },
    {
      title: 'Active Stores',
      value: stats?.['active'] ?? storesData?.items.filter((s) => s.isActive).length ?? 0,
      icon: TrendingUp,
      color: 'text-green-600',
    },
    {
      title: 'Products',
      value: productsData?.pagination.total ?? 0,
      icon: Package,
      color: 'text-purple-600',
    },
  ];

  function txTypeBadgeClass(type: string) {
    switch (type) {
      case 'SALE': return 'bg-blue-100 text-blue-800';
      case 'PURCHASE': return 'bg-green-100 text-green-800';
      case 'RETURN': return 'bg-orange-100 text-orange-800';
      default: return 'bg-zinc-100 text-zinc-800';
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Welcome back, {firstName}!</h1>
        <p className="text-muted-foreground mt-1">Here's an overview of your inventory system.</p>
      </div>

      {totalAlerts > 0 && (
        <div
          className="flex items-center gap-3 p-4 rounded-lg border border-orange-200 bg-orange-50 cursor-pointer hover:bg-orange-100 transition-colors"
          onClick={() => navigate('/alerts')}
        >
          <AlertTriangle className="h-5 w-5 text-orange-600 shrink-0" />
          <div className="flex-1">
            <p className="text-sm font-medium text-orange-900">
              {lowStockCount > 0 && `${lowStockCount} low stock alert${lowStockCount > 1 ? 's' : ''}`}
              {lowStockCount > 0 && expiringSoonCount > 0 && ' · '}
              {expiringSoonCount > 0 && `${expiringSoonCount} item${expiringSoonCount > 1 ? 's' : ''} expiring soon`}
            </p>
            <p className="text-xs text-orange-700 mt-0.5">Click to view and resolve alerts</p>
          </div>
          <Clock className="h-4 w-4 text-orange-500" />
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {statCards.map(({ title, value, icon: Icon, color }) => (
          <Card key={title}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
              <Icon className={`h-5 w-5 ${color}`} />
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold">{value}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="flex flex-wrap gap-3">
        <Button onClick={() => navigate('/stores')}>
          <Plus className="h-4 w-4 mr-2" />
          Add Store
        </Button>
        <Button variant="outline" onClick={() => navigate('/products')}>
          <Plus className="h-4 w-4 mr-2" />
          Add Product
        </Button>
        {firstStoreId && (
          <Button variant="outline" onClick={() => navigate(`/stores/${firstStoreId}/sale`)}>
            <ShoppingCart className="h-4 w-4 mr-2" />
            Record Sale
          </Button>
        )}
        {totalAlerts > 0 && (
          <Button variant="outline" onClick={() => navigate('/alerts')}>
            <AlertTriangle className="h-4 w-4 mr-2" />
            View Alerts
            <Badge className="ml-2 bg-red-500 text-white text-[10px] px-1.5 py-0 h-4">{totalAlerts}</Badge>
          </Button>
        )}
      </div>

      <div>
        <h2 className="text-base font-semibold mb-3">Recent Stores</h2>
        {!storesData?.items.length ? (
          <Card>
            <CardContent className="py-8 text-center text-muted-foreground">
              <p>No stores yet.</p>
              <Button variant="link" className="mt-1" onClick={() => navigate('/stores')}>
                Create your first store
              </Button>
            </CardContent>
          </Card>
        ) : (
          <div className="rounded-md border bg-white overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-zinc-50 border-b">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Name</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Type</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">City</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {storesData.items.slice(0, 5).map((store) => (
                  <tr
                    key={store.id}
                    className="hover:bg-zinc-50 cursor-pointer"
                    onClick={() => navigate(`/stores/${store.id}`)}
                  >
                    <td className="px-4 py-3 font-medium">{store.name}</td>
                    <td className="px-4 py-3 text-muted-foreground capitalize">
                      {store.type.toLowerCase()}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{store.city}</td>
                    <td className="px-4 py-3">
                      <Badge variant={store.isActive ? 'default' : 'secondary'}>
                        {store.isActive ? 'Active' : 'Inactive'}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {recentTxns?.items.length ? (
        <div>
          <h2 className="text-base font-semibold mb-3">Recent Transactions</h2>
          <div className="rounded-md border bg-white overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-zinc-50 border-b">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Date</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Type</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Product</th>
                  <th className="text-right px-4 py-3 font-medium text-muted-foreground">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {recentTxns.items.slice(0, 5).map((tx) => (
                  <tr key={tx.id} className="hover:bg-zinc-50">
                    <td className="px-4 py-3 text-muted-foreground">
                      {format(new Date(tx.createdAt), 'dd MMM yyyy')}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`text-xs font-medium px-2 py-0.5 rounded-full ${txTypeBadgeClass(tx.type)}`}
                      >
                        {tx.type}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{tx.productName ?? '—'}</td>
                    <td className="px-4 py-3 text-right font-medium">
                      ₹{Number(tx.totalAmount).toFixed(2)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}
    </div>
  );
}
