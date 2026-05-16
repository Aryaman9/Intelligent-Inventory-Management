import { useState } from 'react';
import { format } from 'date-fns';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { useStoreTransactions } from '@/hooks/useTransactions';
import { cn } from '@/lib/utils';
import type { Transaction } from '@/lib/types';

type TransactionType = 'ALL' | 'SALE' | 'PURCHASE' | 'RETURN' | 'ADJUSTMENT';

const TYPE_OPTIONS: { label: string; value: TransactionType }[] = [
  { label: 'All Types', value: 'ALL' },
  { label: 'Sale', value: 'SALE' },
  { label: 'Purchase', value: 'PURCHASE' },
  { label: 'Return', value: 'RETURN' },
  { label: 'Adjustment', value: 'ADJUSTMENT' },
];

function typeBadgeVariant(type: Transaction['type']): string {
  switch (type) {
    case 'SALE':
      return 'bg-blue-100 text-blue-700 border-blue-200';
    case 'PURCHASE':
      return 'bg-green-100 text-green-700 border-green-200';
    case 'RETURN':
      return 'bg-orange-100 text-orange-700 border-orange-200';
    case 'ADJUSTMENT':
      return 'bg-gray-100 text-gray-700 border-gray-200';
  }
}

interface TransactionTableProps {
  storeId: string;
}

export function TransactionTable({ storeId }: TransactionTableProps) {
  const [page, setPage] = useState(1);
  const [type, setType] = useState<TransactionType>('ALL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const { data, isLoading } = useStoreTransactions(storeId, page, type, startDate, endDate);

  const transactions = data?.items ?? [];
  const pagination = data?.pagination;
  const totalPages = pagination?.pages ?? 1;

  const handleTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setType(e.target.value as TransactionType);
    setPage(1);
  };

  const handleStartDate = (e: React.ChangeEvent<HTMLInputElement>) => {
    setStartDate(e.target.value);
    setPage(1);
  };

  const handleEndDate = (e: React.ChangeEvent<HTMLInputElement>) => {
    setEndDate(e.target.value);
    setPage(1);
  };

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="flex flex-wrap gap-3 items-end">
        <div className="space-y-1">
          <label className="text-xs font-medium text-muted-foreground">Type</label>
          <select
            value={type}
            onChange={handleTypeChange}
            className="border rounded-md px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          >
            {TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium text-muted-foreground">From</label>
          <input
            type="date"
            value={startDate}
            onChange={handleStartDate}
            className="border rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium text-muted-foreground">To</label>
          <input
            type="date"
            value={endDate}
            onChange={handleEndDate}
            className="border rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
        {(startDate || endDate || type !== 'ALL') && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setType('ALL');
              setStartDate('');
              setEndDate('');
              setPage(1);
            }}
          >
            Clear filters
          </Button>
        )}
      </div>

      {/* Table */}
      <div className="rounded-md border overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-muted/50 border-b">
              <th className="text-left px-4 py-3 font-medium text-muted-foreground whitespace-nowrap">
                Date / Time
              </th>
              <th className="text-left px-4 py-3 font-medium text-muted-foreground">Type</th>
              <th className="text-left px-4 py-3 font-medium text-muted-foreground">Product</th>
              <th className="text-right px-4 py-3 font-medium text-muted-foreground">Quantity</th>
              <th className="text-right px-4 py-3 font-medium text-muted-foreground whitespace-nowrap">
                Unit Price
              </th>
              <th className="text-right px-4 py-3 font-medium text-muted-foreground">Total</th>
              <th className="text-left px-4 py-3 font-medium text-muted-foreground">Payment</th>
              <th className="text-left px-4 py-3 font-medium text-muted-foreground whitespace-nowrap">
                Invoice #
              </th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 8 }).map((_, i) => (
                <tr key={i} className="border-b last:border-0">
                  {Array.from({ length: 8 }).map((__, j) => (
                    <td key={j} className="px-4 py-3">
                      <div className="h-4 bg-zinc-100 rounded animate-pulse" />
                    </td>
                  ))}
                </tr>
              ))
            ) : transactions.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-4 py-12 text-center text-muted-foreground">
                  <p className="font-medium">No transactions found</p>
                  <p className="text-xs mt-1">
                    {type !== 'ALL' || startDate || endDate
                      ? 'Try clearing your filters.'
                      : 'Transactions will appear here once sales or purchases are recorded.'}
                  </p>
                </td>
              </tr>
            ) : (
              transactions.map((t) => (
                <tr key={t.id} className="border-b last:border-0 hover:bg-muted/30 transition-colors">
                  <td className="px-4 py-3 whitespace-nowrap text-muted-foreground">
                    {format(new Date(t.createdAt), 'dd MMM yyyy HH:mm')}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={cn(
                        'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold',
                        typeBadgeVariant(t.type)
                      )}
                    >
                      {t.type}
                    </span>
                  </td>
                  <td className="px-4 py-3 max-w-[180px] truncate">
                    {t.productName ?? '—'}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums">
                    {Number(t.quantity).toFixed(2)}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums">
                    ₹{Number(t.pricePerUnit).toFixed(2)}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums font-medium">
                    ₹{Number(t.totalAmount).toFixed(2)}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {t.paymentMethod ?? '—'}
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                    {t.invoiceNumber ?? '—'}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {pagination && totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            Page {pagination.page} of {totalPages} ({pagination.total} total)
          </span>
          <div className="flex gap-1">
            <Button
              variant="outline"
              size="icon"
              disabled={page <= 1}
              onClick={() => setPage((p) => p - 1)}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="icon"
              disabled={page >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
