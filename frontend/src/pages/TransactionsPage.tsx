import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useStore } from '@/hooks/useStores';
import { TransactionTable } from '@/components/transactions/TransactionTable';

export function TransactionsPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: store } = useStore(id);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => navigate(`/stores/${id}`)}
        >
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-semibold">Transaction History</h1>
          {store && (
            <p className="text-sm text-muted-foreground">{store.name}</p>
          )}
        </div>
      </div>

      {/* Table */}
      <TransactionTable storeId={id} />
    </div>
  );
}
