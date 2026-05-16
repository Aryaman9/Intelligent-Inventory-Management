import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAddInventory } from '@/hooks/useInventory';
import { useProducts } from '@/hooks/useProducts';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

const schema = z.object({
  productId: z.string().min(1, 'Select a product'),
  quantity: z.coerce.number().positive('Quantity must be positive'),
  unit: z.string().min(1, 'Unit is required'),
  costPrice: z.coerce.number().positive('Cost price must be positive'),
  sellingPrice: z.coerce.number().positive('Selling price must be positive'),
  lowStockThreshold: z.coerce.number().min(0).optional(),
  mrp: z.coerce.number().positive().optional().or(z.literal('')),
  expiryDate: z.string().optional(),
  location: z.string().optional(),
});

type FormData = z.infer<typeof schema>;

interface Props {
  storeId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function AddInventoryDialog({ storeId, open, onOpenChange }: Props) {
  const { mutateAsync, isPending } = useAddInventory();
  const { data: productsData, isLoading: productsLoading } = useProducts(1, '', '');
  const { toast } = useToast();

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      unit: 'PCS',
      lowStockThreshold: 10,
    },
  });

  async function onSubmit(values: FormData) {
    try {
      await mutateAsync({
        storeId,
        productId: values.productId,
        quantity: values.quantity,
        unit: values.unit,
        costPrice: values.costPrice,
        sellingPrice: values.sellingPrice,
        lowStockThreshold: values.lowStockThreshold,
        mrp: values.mrp !== '' && values.mrp !== undefined ? Number(values.mrp) : undefined,
        expiryDate: values.expiryDate || undefined,
        location: values.location || undefined,
      });
      toast({ title: 'Inventory added', description: 'Item has been added to inventory.' });
      reset();
      onOpenChange(false);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to add inventory item';
      toast({ variant: 'destructive', title: 'Error', description: msg });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Add Inventory Item</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            {/* Product */}
            <div className="col-span-2 space-y-1">
              <Label>Product</Label>
              <Select onValueChange={(v) => setValue('productId', v)}>
                <SelectTrigger>
                  <SelectValue placeholder={productsLoading ? 'Loading…' : 'Select a product'} />
                </SelectTrigger>
                <SelectContent>
                  {productsData?.items.map((product) => (
                    <SelectItem key={product.id} value={product.id}>
                      {product.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.productId && (
                <p className="text-xs text-destructive">{errors.productId.message}</p>
              )}
            </div>

            {/* Quantity */}
            <div className="space-y-1">
              <Label>Quantity</Label>
              <Input type="number" min="1" step="1" {...register('quantity')} />
              {errors.quantity && (
                <p className="text-xs text-destructive">{errors.quantity.message}</p>
              )}
            </div>

            {/* Unit */}
            <div className="space-y-1">
              <Label>Unit</Label>
              <Input placeholder="PCS" {...register('unit')} />
              {errors.unit && <p className="text-xs text-destructive">{errors.unit.message}</p>}
            </div>

            {/* Cost Price */}
            <div className="space-y-1">
              <Label>Cost Price (₹)</Label>
              <Input type="number" min="0.01" step="0.01" {...register('costPrice')} />
              {errors.costPrice && (
                <p className="text-xs text-destructive">{errors.costPrice.message}</p>
              )}
            </div>

            {/* Selling Price */}
            <div className="space-y-1">
              <Label>Selling Price (₹)</Label>
              <Input type="number" min="0.01" step="0.01" {...register('sellingPrice')} />
              {errors.sellingPrice && (
                <p className="text-xs text-destructive">{errors.sellingPrice.message}</p>
              )}
            </div>

            {/* Low Stock Threshold */}
            <div className="space-y-1">
              <Label>Low Stock Threshold</Label>
              <Input type="number" min="0" step="1" {...register('lowStockThreshold')} />
              {errors.lowStockThreshold && (
                <p className="text-xs text-destructive">{errors.lowStockThreshold.message}</p>
              )}
            </div>

            {/* MRP */}
            <div className="space-y-1">
              <Label>MRP (₹, optional)</Label>
              <Input type="number" min="0.01" step="0.01" {...register('mrp')} />
              {errors.mrp && <p className="text-xs text-destructive">{errors.mrp.message}</p>}
            </div>

            {/* Expiry Date */}
            <div className="space-y-1">
              <Label>Expiry Date (optional)</Label>
              <Input type="date" {...register('expiryDate')} />
            </div>

            {/* Location */}
            <div className="space-y-1">
              <Label>Location (optional)</Label>
              <Input placeholder="Aisle 3, Shelf B" {...register('location')} />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? 'Adding…' : 'Add Item'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
