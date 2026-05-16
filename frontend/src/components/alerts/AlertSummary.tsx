import { AlertTriangle, Clock, CheckCircle } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';

interface AlertSummaryProps {
  summary: {
    lowStockCount: number;
    expiringSoonCount: number;
  };
}

export function AlertSummary({ summary }: AlertSummaryProps) {
  const { lowStockCount, expiringSoonCount } = summary;

  if (lowStockCount === 0 && expiringSoonCount === 0) {
    return (
      <Card className="border-green-200 bg-green-50">
        <CardContent className="p-4 flex items-center gap-3">
          <CheckCircle className="h-6 w-6 text-green-600 shrink-0" />
          <p className="text-sm font-medium text-green-800">All Clear! No active alerts</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-4">
      {/* Low Stock summary */}
      <Card className="border-red-200 bg-red-50">
        <CardContent className="p-4 flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-red-100">
            <AlertTriangle className="h-5 w-5 text-red-600" />
          </div>
          <div>
            <p className="text-2xl font-bold text-red-700">{lowStockCount}</p>
            <p className="text-xs text-red-600 font-medium">Low Stock Items</p>
          </div>
        </CardContent>
      </Card>

      {/* Expiring Soon summary */}
      <Card className="border-yellow-200 bg-yellow-50">
        <CardContent className="p-4 flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-yellow-100">
            <Clock className="h-5 w-5 text-yellow-600" />
          </div>
          <div>
            <p className="text-2xl font-bold text-yellow-700">{expiringSoonCount}</p>
            <p className="text-xs text-yellow-600 font-medium">Expiring Soon</p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
