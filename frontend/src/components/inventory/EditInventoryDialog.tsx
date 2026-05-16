import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useUpdateInventory } from '@/hooks/useInventory';
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
import type { Inventory } from '@/lib/types';

const schema = z.object({
  lowStockThreshold: z.coerce.number().min(0, 'Must be 0 or more'),
  costPrice: z.coerce.number().positive('Cost price must be positive'),
  sellingPrice: z.coerce.number().positive('Selling price must be positive'),
  mrp: z.coerce.number().positive().optional().or(z.literal('')),
  location: z.string().optional(),
});

type FormData = z.infer<typeof schema>;

interface Props {
  item: Inventory;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditInventoryDialog({ item, open, onOpenChange }: Props) {
  const { mutateAsync, isPending } = useUpdateInventory();
  const { toast } = useToast();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      lowStockThreshold: item.lowStockThreshold,
      costPrice: item.costPrice,
      sellingPrice: item.sellingPrice,
      mrp: item.mrp ?? '',
      location: item.location ?? '',
    },
  });

  async function onSubmit(values: FormData) {
    try {
      await mutateAsync({
        id: item.id,
        lowStockThreshold: values.lowStockThreshold,
        costPrice: values.costPrice,
        sellingPrice: values.sellingPrice,
        mrp: values.mrp !== '' && values.mrp !== undefined ? Number(values.mrp) : undefined,
        location: values.location || undefined,
      });
      toast({ title: 'Inventory updated', description: 'Item details have been updated.' });
      onOpenChange(false);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to update inventory item';
      toast({ variant: 'destructive', title: 'Error', description: msg });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Edit Inventory Item</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            {/* Low Stock Threshold */}
            <div className="col-span-2 space-y-1">
              <Label>Low Stock Threshold</Label>
              <Input type="number" min="0" step="1" {...register('lowStockThreshold')} />
              {errors.lowStockThreshold && (
                <p className="text-xs text-destructive">{errors.lowStockThreshold.message}</p>
              )}
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

            {/* MRP */}
            <div className="space-y-1">
              <Label>MRP (₹, optional)</Label>
              <Input type="number" min="0.01" step="0.01" {...register('mrp')} />
              {errors.mrp && <p className="text-xs text-destructive">{errors.mrp.message}</p>}
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
              {isPending ? 'Saving…' : 'Save Changes'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
