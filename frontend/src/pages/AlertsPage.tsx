import { Bell } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAlerts } from '@/hooks/useInventory';
import { AlertsList } from '@/components/alerts/AlertsList';

function AlertsSkeleton() {
  return (
    <div className="space-y-4 animate-pulse">
      {/* Summary skeleton */}
      <div className="grid grid-cols-2 gap-4">
        <div className="h-20 rounded-lg bg-gray-200" />
        <div className="h-20 rounded-lg bg-gray-200" />
      </div>
      {/* Card skeletons */}
      <div className="h-5 w-40 rounded bg-gray-200" />
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-32 rounded-lg bg-gray-200" />
        ))}
      </div>
    </div>
  );
}

export function AlertsPage() {
  const navigate = useNavigate();
  const { data: alertsData, isLoading, isError } = useAlerts();

  function handleRestock(inventoryId: string, storeId: string) {
    navigate(`/stores/${storeId}/purchase?inventoryId=${inventoryId}`);
  }

  return (
    <div className="space-y-6 p-6">
      {/* Page header */}
      <div className="flex items-center gap-3">
        <Bell className="h-6 w-6 text-gray-700" />
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Alerts</h1>
          <p className="text-sm text-muted-foreground">
            Low stock and expiry alerts for all your stores
          </p>
        </div>
      </div>

      {/* Content */}
      {isLoading && <AlertsSkeleton />}

      {isError && !isLoading && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          Failed to load alerts. Please try again.
        </div>
      )}

      {alertsData && !isLoading && (
        <AlertsList data={alertsData} onRestock={handleRestock} />
      )}
    </div>
  );
}
