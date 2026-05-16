import { useRef } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useStoreInventory } from '@/hooks/useInventory';
import { useRecordSale } from '@/hooks/useTransactions';
import { generateIdempotencyKey } from '@/lib/idempotency';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import type { SaleResponse, Inventory } from '@/lib/types';

const schema = z.object({
  inventoryId: z.string().min(1, 'Select an item'),
  quantity: z.coerce.number().positive('Must be positive'),
  pricePerUnit: z.coerce.number().positive('Must be positive'),
  paymentMethod: z.enum(['CASH', 'UPI', 'CARD', 'CREDIT']),
  customerName: z.string().optional(),
  customerPhone: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

const PAYMENT_METHODS = ['CASH', 'UPI', 'CARD', 'CREDIT'] as const;

interface SaleFormProps {
  storeId: string;
  onSuccess: (result: SaleResponse) => void;
}

export function SaleForm({ storeId, onSuccess }: SaleFormProps) {
  const idempotencyKeyRef = useRef<string>(generateIdempotencyKey());
  const { toast } = useToast();
  const { data: inventoryData, isLoading: inventoryLoading } = useStoreInventory(storeId, 1, false);
  const recordSale = useRecordSale();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    control,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      paymentMethod: 'CASH',
      quantity: undefined,
      pricePerUnit: undefined,
    },
  });

  const watchedValues = useWatch({ control });
  const selectedInventoryId = watch('inventoryId');
  const selectedPaymentMethod = watch('paymentMethod');

  const inventoryItems: Inventory[] = inventoryData?.items ?? [];
  const selectedItem = inventoryItems.find((item) => item.id === selectedInventoryId);

  const quantity = Number(watchedValues.quantity) || 0;
  const pricePerUnit = Number(watchedValues.pricePerUnit) || 0;
  const total = quantity * pricePerUnit;

  const handleInventoryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const id = e.target.value;
    setValue('inventoryId', id);
    const item = inventoryItems.find((i) => i.id === id);
    if (item) {
      setValue('pricePerUnit', item.sellingPrice);
    }
  };

  const onSubmit = (values: FormValues) => {
    if (selectedItem && values.quantity > selectedItem.quantity) {
      toast({
        title: 'Insufficient stock',
        description: `Only ${selectedItem.quantity} ${selectedItem.unit} available.`,
        variant: 'destructive',
      });
      return;
    }

    recordSale.mutate(
      {
        data: {
          inventoryId: values.inventoryId,
          quantity: values.quantity,
          pricePerUnit: values.pricePerUnit,
          paymentMethod: values.paymentMethod,
          customerName: values.customerName || undefined,
          customerPhone: values.customerPhone || undefined,
        },
        idempotencyKey: idempotencyKeyRef.current,
      },
      {
        onSuccess: (response) => {
          onSuccess(response.data);
        },
        onError: (error) => {
          const axiosError = error as { response?: { data?: { error?: string; message?: string } } };
          const msg =
            axiosError.response?.data?.error ||
            axiosError.response?.data?.message ||
            'Failed to record sale. Please try again.';
          const isStockError =
            msg.toLowerCase().includes('insufficient') ||
            msg.toLowerCase().includes('stock') ||
            msg.toLowerCase().includes('quantity');
          toast({
            title: isStockError ? 'Insufficient Stock' : 'Sale Failed',
            description: msg,
            variant: 'destructive',
          });
        },
      }
    );
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Record Sale</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {/* Inventory item */}
          <div className="space-y-1.5">
            <Label htmlFor="inventoryId">Product</Label>
            {inventoryLoading ? (
              <div className="h-9 bg-zinc-100 rounded animate-pulse" />
            ) : (
              <select
                id="inventoryId"
                className={cn(
                  'w-full border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring',
                  errors.inventoryId && 'border-red-500'
                )}
                value={selectedInventoryId ?? ''}
                onChange={handleInventoryChange}
              >
                <option value="">Select a product...</option>
                {inventoryItems.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.productName ?? 'Unknown'} (Available:{' '}
                    {Number(item.quantity).toFixed(2)} {item.unit})
                  </option>
                ))}
              </select>
            )}
            {errors.inventoryId && (
              <p className="text-xs text-red-500">{errors.inventoryId.message}</p>
            )}
          </div>

          {/* Quantity */}
          <div className="space-y-1.5">
            <Label htmlFor="quantity">Quantity</Label>
            <Input
              id="quantity"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="0"
              {...register('quantity')}
              className={cn(errors.quantity && 'border-red-500')}
            />
            {selectedItem && (
              <p className="text-xs text-muted-foreground">
                Available: {Number(selectedItem.quantity).toFixed(2)} {selectedItem.unit}
              </p>
            )}
            {errors.quantity && (
              <p className="text-xs text-red-500">{errors.quantity.message}</p>
            )}
          </div>

          {/* Price per unit */}
          <div className="space-y-1.5">
            <Label htmlFor="pricePerUnit">Price per Unit (₹)</Label>
            <Input
              id="pricePerUnit"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="0.00"
              {...register('pricePerUnit')}
              className={cn(errors.pricePerUnit && 'border-red-500')}
            />
            {errors.pricePerUnit && (
              <p className="text-xs text-red-500">{errors.pricePerUnit.message}</p>
            )}
          </div>

          {/* Total */}
          <div className="rounded-md bg-blue-50 border border-blue-200 px-4 py-3 flex justify-between items-center">
            <span className="text-sm font-medium text-blue-700">Total Amount</span>
            <span className="text-xl font-bold text-blue-800">₹{total.toFixed(2)}</span>
          </div>

          {/* Payment method */}
          <div className="space-y-1.5">
            <Label>Payment Method</Label>
            <div className="flex gap-2 flex-wrap">
              {PAYMENT_METHODS.map((method) => (
                <button
                  key={method}
                  type="button"
                  onClick={() => setValue('paymentMethod', method)}
                  className={cn(
                    'px-4 py-2 rounded-full text-sm font-medium border transition-colors',
                    selectedPaymentMethod === method
                      ? 'bg-primary text-primary-foreground border-primary'
                      : 'bg-white text-muted-foreground border-input hover:bg-accent'
                  )}
                >
                  {method}
                </button>
              ))}
            </div>
            {errors.paymentMethod && (
              <p className="text-xs text-red-500">{errors.paymentMethod.message}</p>
            )}
          </div>

          {/* Customer Name */}
          <div className="space-y-1.5">
            <Label htmlFor="customerName">Customer Name (optional)</Label>
            <Input
              id="customerName"
              placeholder="Walk-in customer"
              {...register('customerName')}
            />
          </div>

          {/* Customer Phone */}
          <div className="space-y-1.5">
            <Label htmlFor="customerPhone">Customer Phone (optional)</Label>
            <Input
              id="customerPhone"
              type="tel"
              placeholder="+91 XXXXX XXXXX"
              {...register('customerPhone')}
            />
          </div>

          <Button
            type="submit"
            className="w-full"
            disabled={recordSale.isPending}
          >
            {recordSale.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Record Sale
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
