import { format } from 'date-fns';
import { AlertTriangle, Clock, Package } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { LowStockAlert, ExpiryAlert } from '@/lib/types';

// ─── Low Stock Card ──────────────────────────────────────────────────────────

interface LowStockCardProps {
  alert: LowStockAlert;
  onRestock: (inventoryId: string, storeId: string) => void;
}

export function LowStockCard({ alert, onRestock }: LowStockCardProps) {
  const { quantity, lowStockThreshold, shortage, product, store } = alert;
  const fillPercent = Math.min(quantity / (lowStockThreshold * 2), 1) * 100;
  const barColor = fillPercent < 30 ? 'bg-red-500' : 'bg-yellow-500';

  return (
    <Card className="border-red-200 bg-red-50">
      <CardContent className="p-4 space-y-3">
        {/* Header */}
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2 min-w-0">
            <Package className="h-4 w-4 text-red-600 shrink-0" />
            <div className="min-w-0">
              <p className="font-semibold text-sm text-gray-900 truncate">{product.name}</p>
              <p className="text-xs text-gray-500 truncate">{store.name}</p>
            </div>
          </div>
          <AlertTriangle className="h-4 w-4 text-red-500 shrink-0 mt-0.5" />
        </div>

        {/* Quantity info */}
        <div className="space-y-1">
          <div className="flex justify-between text-xs text-gray-600">
            <span>
              Stock: <span className="font-medium text-gray-800">{quantity}</span> /{' '}
              {lowStockThreshold} units
            </span>
            <span className="text-red-600 font-medium">Shortage: {shortage} units</span>
          </div>

          {/* Progress bar */}
          <div className="h-1.5 w-full rounded-full bg-red-100 overflow-hidden">
            <div
              className={cn('h-full rounded-full transition-all', barColor)}
              style={{ width: `${fillPercent}%` }}
            />
          </div>
        </div>

        {/* Restock button */}
        <Button
          size="sm"
          variant="outline"
          className="w-full border-red-300 text-red-700 hover:bg-red-100 hover:text-red-800"
          onClick={() => onRestock(alert.inventoryId, store.id)}
        >
          Restock
        </Button>
      </CardContent>
    </Card>
  );
}

// ─── Expiry Card ─────────────────────────────────────────────────────────────

interface ExpiryCardProps {
  alert: ExpiryAlert;
}

export function ExpiryCard({ alert }: ExpiryCardProps) {
  const { expiryDate, daysUntilExpiry, product, store } = alert;

  const isUrgent = daysUntilExpiry < 3;
  const isWarning = daysUntilExpiry >= 3 && daysUntilExpiry <= 7;

  const borderClass = isUrgent
    ? 'border-red-200 bg-red-50'
    : 'border-yellow-200 bg-yellow-50';

  const daysColor = isUrgent
    ? 'text-red-600'
    : isWarning
    ? 'text-yellow-600'
    : 'text-orange-500';

  const formattedDate = format(new Date(expiryDate), 'dd MMM yyyy');

  return (
    <Card className={cn('border', borderClass)}>
      <CardContent className="p-4 space-y-3">
        {/* Header */}
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2 min-w-0">
            <Package className="h-4 w-4 text-gray-500 shrink-0" />
            <div className="min-w-0">
              <p className="font-semibold text-sm text-gray-900 truncate">{product.name}</p>
              <p className="text-xs text-gray-500 truncate">{store.name}</p>
            </div>
          </div>
          <Clock className={cn('h-4 w-4 shrink-0 mt-0.5', daysColor)} />
        </div>

        {/* Expiry info */}
        <div className="space-y-1">
          <div className="flex justify-between text-xs">
            <span className="text-gray-600">
              Expires: <span className="font-medium text-gray-800">{formattedDate}</span>
            </span>
            <span className={cn('font-semibold', daysColor)}>
              {daysUntilExpiry} day{daysUntilExpiry !== 1 ? 's' : ''} left
            </span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
