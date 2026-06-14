import { useRef, useEffect } from 'react';
import { useForm, useWatch, type Resolver } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useStoreInventory } from '@/hooks/useInventory';
import { useRecordPurchase } from '@/hooks/useTransactions';
import { generateIdempotencyKey } from '@/lib/idempotency';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import type { Inventory } from '@/lib/types';

const schema = z.object({
  inventoryId: z.string().min(1, 'Select an item'),
  quantity: z.coerce.number().positive('Must be positive'),
  pricePerUnit: z.coerce.number().positive('Must be positive'),
  notes: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

interface PurchaseSuccessResult {
  inventoryQuantity: string;
  productName: string;
}

interface PurchaseFormProps {
  storeId: string;
  preselectedInventoryId?: string;
  onSuccess: (result: PurchaseSuccessResult) => void;
}

export function PurchaseForm({ storeId, preselectedInventoryId, onSuccess }: PurchaseFormProps) {
  const idempotencyKeyRef = useRef<string>(generateIdempotencyKey());
  const { toast } = useToast();
  const { data: inventoryData, isLoading: inventoryLoading } = useStoreInventory(storeId, 1, false);
  const recordPurchase = useRecordPurchase();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    control,
    formState: { errors },
  } = useForm<FormValues>({
    // zod v4's z.coerce.number() has an `unknown` input type, so cast the resolver to the
    // form's value type to satisfy react-hook-form's generics. Runtime behavior is unchanged.
    resolver: zodResolver(schema) as unknown as Resolver<FormValues>,
    defaultValues: {
      inventoryId: preselectedInventoryId ?? '',
      quantity: undefined,
      pricePerUnit: undefined,
      notes: '',
    },
  });

  const watchedValues = useWatch({ control });
  const selectedInventoryId = watch('inventoryId');

  const inventoryItems: Inventory[] = inventoryData?.items ?? [];
  const selectedItem = inventoryItems.find((item) => item.id === selectedInventoryId);

  // Pre-fill cost price when item is selected or pre-selected
  useEffect(() => {
    if (selectedItem) {
      setValue('pricePerUnit', selectedItem.costPrice);
    }
  }, [selectedItem, setValue]);

  // Apply preselectedInventoryId when inventory loads
  useEffect(() => {
    if (preselectedInventoryId && inventoryItems.length > 0) {
      setValue('inventoryId', preselectedInventoryId);
    }
  }, [preselectedInventoryId, inventoryItems, setValue]);

  const quantity = Number(watchedValues.quantity) || 0;
  const pricePerUnit = Number(watchedValues.pricePerUnit) || 0;
  const total = quantity * pricePerUnit;

  const handleInventoryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const id = e.target.value;
    setValue('inventoryId', id);
    const item = inventoryItems.find((i) => i.id === id);
    if (item) {
      setValue('pricePerUnit', item.costPrice);
    }
  };

  const onSubmit = (values: FormValues) => {
    recordPurchase.mutate(
      {
        data: {
          inventoryId: values.inventoryId,
          quantity: values.quantity,
          pricePerUnit: values.pricePerUnit,
          notes: values.notes || undefined,
        },
        idempotencyKey: idempotencyKeyRef.current,
      },
      {
        onSuccess: (response) => {
          onSuccess({
            inventoryQuantity: response.data.inventoryQuantity,
            productName: response.data.productName,
          });
        },
        onError: (error) => {
          const axiosError = error as { response?: { data?: { error?: string; message?: string } } };
          const msg =
            axiosError.response?.data?.error ||
            axiosError.response?.data?.message ||
            'Failed to record purchase. Please try again.';
          toast({
            title: 'Purchase Failed',
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
        <CardTitle>Record Purchase / Restock</CardTitle>
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
                    {item.productName ?? 'Unknown'} (Current stock:{' '}
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
            <Label htmlFor="quantity">Quantity to Purchase</Label>
            <Input
              id="quantity"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="0"
              {...register('quantity')}
              className={cn(errors.quantity && 'border-red-500')}
            />
            {errors.quantity && (
              <p className="text-xs text-red-500">{errors.quantity.message}</p>
            )}
          </div>

          {/* Cost price */}
          <div className="space-y-1.5">
            <Label htmlFor="pricePerUnit">Cost Price per Unit (₹)</Label>
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
          <div className="rounded-md bg-green-50 border border-green-200 px-4 py-3 flex justify-between items-center">
            <span className="text-sm font-medium text-green-700">Total Cost</span>
            <span className="text-xl font-bold text-green-800">₹{total.toFixed(2)}</span>
          </div>

          {/* Notes */}
          <div className="space-y-1.5">
            <Label htmlFor="notes">Notes (optional)</Label>
            <textarea
              id="notes"
              placeholder="Supplier name, batch info, etc."
              {...register('notes')}
              className="w-full border rounded-md px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-ring min-h-[80px]"
            />
          </div>

          <Button
            type="submit"
            className="w-full bg-green-600 hover:bg-green-700 text-white"
            disabled={recordPurchase.isPending}
          >
            {recordPurchase.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Record Purchase
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
