import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { TransactionStats } from '@/lib/types';

interface StatsCardsProps {
  stats: TransactionStats;
}

export function StatsCards({ stats }: StatsCardsProps) {
  const avgOrderValue = (
    parseFloat(stats.totalRevenue || '0') / (stats.totalSalesCount || 1)
  ).toFixed(2);

  const cards = [
    {
      title: 'Total Revenue',
      value: `₹${parseFloat(stats.totalRevenue || '0').toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      })}`,
    },
    {
      title: 'Total Transactions',
      value: stats.totalTransactions.toLocaleString('en-IN'),
    },
    {
      title: 'Avg Order Value',
      value: `₹${parseFloat(avgOrderValue).toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      })}`,
    },
    {
      title: 'Profit Margin',
      value: `${stats.profitMargin}%`,
    },
  ];

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => (
        <Card key={card.title}>
          <CardHeader className="pb-1">
            <CardTitle className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
              {card.title}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold text-gray-900">{card.value}</p>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
