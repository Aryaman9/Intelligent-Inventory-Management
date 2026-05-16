import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import type { Store } from '@/lib/types';
import { useUpdateStore } from '@/hooks/useStores';
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
  name: z.string().min(1, 'Name is required'),
  type: z.string().min(1, 'Type is required'),
  address: z.string().min(1, 'Address is required'),
  city: z.string().min(1, 'City is required'),
  state: z.string().min(1, 'State is required'),
  pincode: z.string().min(6).max(6),
  gstin: z.string().optional(),
  phone: z.string().optional(),
  email: z.string().email().optional().or(z.literal('')),
});

type FormData = z.infer<typeof schema>;

const STORE_TYPES = ['GROCERY', 'PHARMACY', 'ELECTRONICS', 'CLOTHING', 'RESTAURANT', 'OTHER'];

interface Props {
  store: Store;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function EditStoreDialog({ store, open, onOpenChange }: Props) {
  const { mutateAsync, isPending } = useUpdateStore();
  const { toast } = useToast();

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (store) {
      reset({
        name: store.name,
        type: store.type,
        address: store.address,
        city: store.city,
        state: store.state,
        pincode: store.pincode,
        gstin: store.gstin ?? '',
        phone: store.phone ?? '',
        email: store.email ?? '',
      });
    }
  }, [store, reset]);

  async function onSubmit(values: FormData) {
    try {
      await mutateAsync({ id: store.id, ...values });
      toast({ title: 'Store updated' });
      onOpenChange(false);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Update failed';
      toast({ variant: 'destructive', title: 'Error', description: msg });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Edit Store</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2 space-y-1">
              <Label>Store Name</Label>
              <Input {...register('name')} />
              {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
            </div>
            <div className="space-y-1">
              <Label>Type</Label>
              <Select defaultValue={store.type} onValueChange={(v) => setValue('type', v)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {STORE_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>
                      {t.charAt(0) + t.slice(1).toLowerCase()}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>GSTIN (optional)</Label>
              <Input {...register('gstin')} />
            </div>
            <div className="col-span-2 space-y-1">
              <Label>Address</Label>
              <Input {...register('address')} />
              {errors.address && <p className="text-xs text-destructive">{errors.address.message}</p>}
            </div>
            <div className="space-y-1">
              <Label>City</Label>
              <Input {...register('city')} />
            </div>
            <div className="space-y-1">
              <Label>State</Label>
              <Input {...register('state')} />
            </div>
            <div className="space-y-1">
              <Label>Pincode</Label>
              <Input {...register('pincode')} />
            </div>
            <div className="space-y-1">
              <Label>Phone</Label>
              <Input {...register('phone')} />
            </div>
            <div className="col-span-2 space-y-1">
              <Label>Email</Label>
              <Input {...register('email')} />
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
