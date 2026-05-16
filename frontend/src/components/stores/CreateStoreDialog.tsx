import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreateStore } from '@/hooks/useStores';
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
  pincode: z.string().min(6, 'Valid pincode required').max(6),
  gstin: z.string().optional(),
  phone: z.string().optional(),
  email: z.string().email().optional().or(z.literal('')),
});

type FormData = z.infer<typeof schema>;

const STORE_TYPES = ['GROCERY', 'PHARMACY', 'ELECTRONICS', 'CLOTHING', 'RESTAURANT', 'OTHER'];

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateStoreDialog({ open, onOpenChange }: Props) {
  const { mutateAsync, isPending } = useCreateStore();
  const { toast } = useToast();

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  async function onSubmit(values: FormData) {
    try {
      await mutateAsync(values);
      toast({ title: 'Store created', description: `${values.name} has been added.` });
      reset();
      onOpenChange(false);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to create store';
      toast({ variant: 'destructive', title: 'Error', description: msg });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Add Store</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2 space-y-1">
              <Label>Store Name</Label>
              <Input placeholder="My Store" {...register('name')} />
              {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
            </div>
            <div className="space-y-1">
              <Label>Type</Label>
              <Select onValueChange={(v) => setValue('type', v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select type" />
                </SelectTrigger>
                <SelectContent>
                  {STORE_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>
                      {t.charAt(0) + t.slice(1).toLowerCase()}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.type && <p className="text-xs text-destructive">{errors.type.message}</p>}
            </div>
            <div className="space-y-1">
              <Label>GSTIN (optional)</Label>
              <Input placeholder="22AAAAA0000A1Z5" {...register('gstin')} />
            </div>
            <div className="col-span-2 space-y-1">
              <Label>Address</Label>
              <Input placeholder="Street address" {...register('address')} />
              {errors.address && <p className="text-xs text-destructive">{errors.address.message}</p>}
            </div>
            <div className="space-y-1">
              <Label>City</Label>
              <Input placeholder="Mumbai" {...register('city')} />
              {errors.city && <p className="text-xs text-destructive">{errors.city.message}</p>}
            </div>
            <div className="space-y-1">
              <Label>State</Label>
              <Input placeholder="Maharashtra" {...register('state')} />
              {errors.state && <p className="text-xs text-destructive">{errors.state.message}</p>}
            </div>
            <div className="space-y-1">
              <Label>Pincode</Label>
              <Input placeholder="400001" {...register('pincode')} />
              {errors.pincode && <p className="text-xs text-destructive">{errors.pincode.message}</p>}
            </div>
            <div className="space-y-1">
              <Label>Phone (optional)</Label>
              <Input placeholder="+91 98765 43210" {...register('phone')} />
            </div>
            <div className="col-span-2 space-y-1">
              <Label>Email (optional)</Label>
              <Input placeholder="store@example.com" {...register('email')} />
              {errors.email && <p className="text-xs text-destructive">{errors.email.message}</p>}
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? 'Creating…' : 'Create Store'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
