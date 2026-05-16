import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Plus, Trash2 } from 'lucide-react';
import { useCreateProduct } from '@/hooks/useProducts';
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
import type { ProductVariant } from '@/lib/types';

const CATEGORIES = [
  'FOOD', 'BEVERAGE', 'DAIRY', 'BAKERY', 'PERSONAL_CARE',
  'HOUSEHOLD', 'ELECTRONICS', 'CLOTHING', 'PHARMA', 'OTHER',
];

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  category: z.string().min(1, 'Category is required'),
  brand: z.string().optional(),
  barcode: z.string().optional(),
  tags: z.string().optional(),
});

type FormData = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateProductDialog({ open, onOpenChange }: Props) {
  const { mutateAsync, isPending } = useCreateProduct();
  const { toast } = useToast();
  const [variants, setVariants] = useState<ProductVariant[]>([]);

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  function addVariant() {
    setVariants((prev) => [...prev, { size: '', unit: 'kg', mrp: 0 }]);
  }

  function removeVariant(idx: number) {
    setVariants((prev) => prev.filter((_, i) => i !== idx));
  }

  function updateVariant(idx: number, field: keyof ProductVariant, value: string | number) {
    setVariants((prev) =>
      prev.map((v, i) => (i === idx ? { ...v, [field]: value } : v))
    );
  }

  async function onSubmit(values: FormData) {
    try {
      const tags = values.tags ? values.tags.split(',').map((t) => t.trim()).filter(Boolean) : [];
      await mutateAsync({
        name: values.name,
        category: values.category,
        brand: values.brand,
        barcode: values.barcode,
        tags,
        variants: variants.length ? variants : undefined,
      });
      toast({ title: 'Product created', description: `${values.name} has been added.` });
      reset();
      setVariants([]);
      onOpenChange(false);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to create product';
      toast({ variant: 'destructive', title: 'Error', description: msg });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Add Product</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2 space-y-1">
              <Label>Product Name</Label>
              <Input placeholder="Amul Butter 500g" {...register('name')} />
              {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
            </div>
            <div className="space-y-1">
              <Label>Category</Label>
              <Select onValueChange={(v) => setValue('category', v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select category" />
                </SelectTrigger>
                <SelectContent>
                  {CATEGORIES.map((c) => (
                    <SelectItem key={c} value={c}>
                      {c.charAt(0) + c.slice(1).toLowerCase().replace('_', ' ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.category && <p className="text-xs text-destructive">{errors.category.message}</p>}
            </div>
            <div className="space-y-1">
              <Label>Brand (optional)</Label>
              <Input placeholder="Amul" {...register('brand')} />
            </div>
            <div className="space-y-1">
              <Label>Barcode (optional)</Label>
              <Input placeholder="8901234567890" {...register('barcode')} />
            </div>
            <div className="space-y-1">
              <Label>Tags (comma-separated)</Label>
              <Input placeholder="dairy, butter, spread" {...register('tags')} />
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Variants (optional)</Label>
              <Button type="button" variant="outline" size="sm" onClick={addVariant}>
                <Plus className="h-3 w-3 mr-1" />
                Add Variant
              </Button>
            </div>
            {variants.map((v, i) => (
              <div key={i} className="flex items-center gap-2 p-2 border rounded">
                <Input
                  placeholder="Size (e.g. 500)"
                  value={v.size}
                  onChange={(e) => updateVariant(i, 'size', e.target.value)}
                  className="w-20"
                />
                <Input
                  placeholder="Unit"
                  value={v.unit}
                  onChange={(e) => updateVariant(i, 'unit', e.target.value)}
                  className="w-16"
                />
                <Input
                  placeholder="MRP"
                  type="number"
                  value={v.mrp}
                  onChange={(e) => updateVariant(i, 'mrp', parseFloat(e.target.value))}
                  className="w-24"
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => removeVariant(i)}
                  className="shrink-0 text-destructive"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ))}
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? 'Creating…' : 'Create Product'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
