import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useStore } from '@/hooks/useStores';
import { SaleForm } from '@/components/transactions/SaleForm';
import { InvoiceCard } from '@/components/transactions/InvoiceCard';
import type { SaleResponse } from '@/lib/types';

export function SalePage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: store } = useStore(id);
  const [saleResult, setSaleResult] = useState<SaleResponse | null>(null);

  const handleSuccess = (result: SaleResponse) => {
    setSaleResult(result);
  };

  const handleReset = () => {
    setSaleResult(null);
  };

  return (
    <div className="space-y-6 max-w-xl mx-auto">
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
          <h1 className="text-2xl font-semibold">Record Sale</h1>
          {store && (
            <p className="text-sm text-muted-foreground">{store.name}</p>
          )}
        </div>
      </div>

      {/* Content */}
      {saleResult ? (
        <InvoiceCard result={saleResult} onRecordAnother={handleReset} />
      ) : (
        <SaleForm storeId={id} onSuccess={handleSuccess} />
      )}
    </div>
  );
}
