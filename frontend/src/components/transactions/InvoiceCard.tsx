import { CheckCircle2 } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import type { SaleResponse } from '@/lib/types';

interface InvoiceCardProps {
  result: SaleResponse;
  onRecordAnother: () => void;
}

export function InvoiceCard({ result, onRecordAnother }: InvoiceCardProps) {
  const total = parseFloat(result.transaction.totalAmount);
  const qty = parseFloat(result.transaction.quantity);
  const remaining = parseFloat(result.inventoryQuantity);

  return (
    <Card className="border-green-200 bg-green-50">
      <CardContent className="pt-8 pb-8 flex flex-col items-center text-center gap-4">
        <CheckCircle2 className="h-14 w-14 text-green-600" />

        <div>
          <h2 className="text-xl font-semibold text-green-800">Sale Recorded Successfully!</h2>
          <p className="text-sm text-green-700 mt-1">Transaction has been saved.</p>
        </div>

        <div className="w-full max-w-sm rounded-lg bg-white border border-green-200 divide-y divide-green-100">
          <div className="flex justify-between px-4 py-3 text-sm">
            <span className="text-muted-foreground">Invoice</span>
            <span className="font-mono font-semibold">{result.transaction.invoiceNumber}</span>
          </div>
          <div className="flex justify-between px-4 py-3 text-sm">
            <span className="text-muted-foreground">Product</span>
            <span className="font-medium">{result.productName}</span>
          </div>
          <div className="flex justify-between px-4 py-3 text-sm">
            <span className="text-muted-foreground">Quantity Sold</span>
            <span className="font-medium">{qty.toFixed(2)} units</span>
          </div>
          <div className="flex justify-between px-4 py-3">
            <span className="text-muted-foreground text-sm">Total Amount</span>
            <span className="text-lg font-bold text-green-700">
              ₹{total.toFixed(2)}
            </span>
          </div>
          <div className="flex justify-between px-4 py-3 text-sm bg-green-50 rounded-b-lg">
            <span className="text-muted-foreground">Remaining Stock</span>
            <span className="font-medium">{remaining.toFixed(2)} units</span>
          </div>
        </div>

        <Button
          onClick={onRecordAnother}
          className="mt-2 bg-green-600 hover:bg-green-700 text-white"
        >
          Record Another Sale
        </Button>
      </CardContent>
    </Card>
  );
}
