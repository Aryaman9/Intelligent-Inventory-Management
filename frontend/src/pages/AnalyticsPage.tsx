import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { ArrowLeft, BarChart2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useStore } from '@/hooks/useStores';
import { useTransactionStats } from '@/hooks/useTransactions';
import { DateRangePicker } from '@/components/analytics/DateRangePicker';
import { StatsCards } from '@/components/analytics/StatsCards';
import { RevenueChart } from '@/components/analytics/RevenueChart';
import { PaymentBreakdown } from '@/components/analytics/PaymentBreakdown';

function AnalyticsSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {/* Stats cards skeleton */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-24 rounded-lg bg-gray-200" />
        ))}
      </div>
      {/* Charts skeleton */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="h-72 rounded-lg bg-gray-200" />
        <div className="h-72 rounded-lg bg-gray-200" />
      </div>
    </div>
  );
}

export function AnalyticsPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [startDate, setStartDate] = useState<string>(() => {
    const d = new Date();
    d.setDate(d.getDate() - 30);
    return format(d, 'yyyy-MM-dd');
  });
  const [endDate, setEndDate] = useState<string>(() => format(new Date(), 'yyyy-MM-dd'));

  const { data: store } = useStore(id);
  const { data: stats, isLoading, isError } = useTransactionStats(id, startDate, endDate);

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => navigate(`/stores/${id}`)}
          aria-label="Back to store"
        >
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <BarChart2 className="h-6 w-6 text-gray-700" />
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Analytics</h1>
          {store && (
            <p className="text-sm text-muted-foreground">{store.name}</p>
          )}
        </div>
      </div>

      {/* Date range picker */}
      <DateRangePicker
        startDate={startDate}
        endDate={endDate}
        onStartChange={setStartDate}
        onEndChange={setEndDate}
      />

      {/* Content */}
      {isLoading && <AnalyticsSkeleton />}

      {isError && !isLoading && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          Failed to load analytics data. Please try again.
        </div>
      )}

      {stats && !isLoading && (
        <div className="space-y-6">
          {/* KPI cards */}
          <StatsCards stats={stats} />

          {/* Charts */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-sm font-semibold">Daily Revenue</CardTitle>
              </CardHeader>
              <CardContent>
                <RevenueChart data={stats.dailyRevenue} />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-sm font-semibold">Payment Methods</CardTitle>
              </CardHeader>
              <CardContent>
                <PaymentBreakdown breakdown={stats.paymentMethodBreakdown} />
              </CardContent>
            </Card>
          </div>
        </div>
      )}
    </div>
  );
}
