import { CheckCircle } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { AlertSummary } from './AlertSummary';
import { LowStockCard, ExpiryCard } from './AlertCard';
import type { AlertsData } from '@/lib/types';

interface AlertsListProps {
  data: AlertsData;
  onRestock: (inventoryId: string, storeId: string) => void;
}

export function AlertsList({ data, onRestock }: AlertsListProps) {
  const { lowStockAlerts, expiryAlerts, summary } = data;
  const hasAlerts = lowStockAlerts.length > 0 || expiryAlerts.length > 0;

  if (!hasAlerts) {
    return (
      <div className="space-y-4">
        <AlertSummary summary={summary} />
        <div className="flex flex-col items-center justify-center py-16 text-center gap-3">
          <CheckCircle className="h-12 w-12 text-green-500" />
          <p className="text-lg font-semibold text-gray-700">All good!</p>
          <p className="text-sm text-gray-500">No alerts at this time.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Summary */}
      <AlertSummary summary={summary} />

      {/* Low Stock Alerts */}
      {lowStockAlerts.length > 0 && (
        <section className="space-y-3">
          <div className="flex items-center gap-2">
            <h2 className="text-base font-semibold text-gray-800">Low Stock Alerts</h2>
            <Badge variant="destructive">{lowStockAlerts.length}</Badge>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {lowStockAlerts.map((alert) => (
              <LowStockCard
                key={alert.inventoryId}
                alert={alert}
                onRestock={onRestock}
              />
            ))}
          </div>
        </section>
      )}

      {/* Expiry Alerts */}
      {expiryAlerts.length > 0 && (
        <section className="space-y-3">
          <div className="flex items-center gap-2">
            <h2 className="text-base font-semibold text-gray-800">Expiry Alerts</h2>
            <Badge className="bg-yellow-500 text-white hover:bg-yellow-600">
              {expiryAlerts.length}
            </Badge>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {expiryAlerts.map((alert) => (
              <ExpiryCard key={alert.inventoryId} alert={alert} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
