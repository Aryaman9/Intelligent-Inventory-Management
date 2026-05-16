import { useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { ArrowLeft, CheckCircle2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { useStore } from '@/hooks/useStores';
import { PurchaseForm } from '@/components/transactions/PurchaseForm';

interface PurchaseSuccessResult {
  inventoryQuantity: string;
  productName: string;
}

export function PurchasePage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { data: store } = useStore(id);

  const searchParams = new URLSearchParams(location.search);
  const preselectedId = searchParams.get('inventoryId') ?? undefined;

  const [successResult, setSuccessResult] = useState<PurchaseSuccessResult | null>(null);

  const handleSuccess = (result: PurchaseSuccessResult) => {
    setSuccessResult(result);
  };

  const handleReset = () => {
    setSuccessResult(null);
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
          <h1 className="text-2xl font-semibold">Restock / Purchase</h1>
          {store && (
            <p className="text-sm text-muted-foreground">{store.name}</p>
          )}
        </div>
      </div>

      {/* Content */}
      {successResult ? (
        <Card className="border-green-200 bg-green-50">
          <CardContent className="pt-8 pb-8 flex flex-col items-center text-center gap-4">
            <CheckCircle2 className="h-14 w-14 text-green-600" />
            <div>
              <h2 className="text-xl font-semibold text-green-800">Inventory Restocked!</h2>
              <p className="text-sm text-green-700 mt-1">Purchase recorded successfully.</p>
            </div>
            <div className="w-full max-w-sm rounded-lg bg-white border border-green-200 divide-y divide-green-100">
              <div className="flex justify-between px-4 py-3 text-sm">
                <span className="text-muted-foreground">Product</span>
                <span className="font-medium">{successResult.productName}</span>
              </div>
              <div className="flex justify-between px-4 py-3 text-sm bg-green-50 rounded-b-lg">
                <span className="text-muted-foreground">New Stock Level</span>
                <span className="font-bold text-green-700">
                  {Number(successResult.inventoryQuantity).toFixed(2)} units
                </span>
              </div>
            </div>
            <Button
              onClick={handleReset}
              className="mt-2 bg-green-600 hover:bg-green-700 text-white"
            >
              Record Another Purchase
            </Button>
          </CardContent>
        </Card>
      ) : (
        <PurchaseForm
          storeId={id}
          preselectedInventoryId={preselectedId}
          onSuccess={handleSuccess}
        />
      )}
    </div>
  );
}
