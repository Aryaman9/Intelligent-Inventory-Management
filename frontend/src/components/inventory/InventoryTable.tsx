import { useState } from 'react';
import { format, differenceInDays } from 'date-fns';
import { Plus, Search, Pencil, Trash2, AlertTriangle } from 'lucide-react';
import { useStoreInventory, useDeleteInventory } from '@/hooks/useInventory';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from '@/components/ui/dialog';
import { StockBadge } from './StockBadge';
import { LowStockIndicator } from './LowStockIndicator';
import { AddInventoryDialog } from './AddInventoryDialog';
import { EditInventoryDialog } from './EditInventoryDialog';
import { cn } from '@/lib/utils';
import type { Inventory } from '@/lib/types';

interface Props {
  storeId: string;
}

export function InventoryTable({ storeId }: Props) {
  const [page, setPage] = useState(1);
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [search, setSearch] = useState('');
  const [addOpen, setAddOpen] = useState(false);
  const [editItem, setEditItem] = useState<Inventory | null>(null);
  const [deleteItem, setDeleteItem] = useState<Inventory | null>(null);

  const { data, isLoading } = useStoreInventory(storeId, page, lowStockOnly);
  const { mutateAsync: deleteInventory, isPending: isDeleting } = useDeleteInventory();
  const { toast } = useToast();

  const filteredItems =
    data?.items.filter((item) =>
      search ? item.productName?.toLowerCase().includes(search.toLowerCase()) : true
    ) ?? [];

  async function confirmDelete() {
    if (!deleteItem) return;
    try {
      await deleteInventory(deleteItem.id);
      toast({ title: 'Item deleted', description: 'Inventory item has been removed.' });
      setDeleteItem(null);
    } catch {
      toast({ variant: 'destructive', title: 'Error', description: 'Failed to delete item' });
    }
  }

  function formatExpiryDate(dateStr: string) {
    const date = new Date(dateStr);
    const days = differenceInDays(date, new Date());
    const formatted = format(date, 'dd MMM yyyy');
    const isNear = days < 7;
    return { formatted, isNear };
  }

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3 justify-between">
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="h-4 w-4 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <Input
              className="pl-8 w-56"
              placeholder="Search products…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <Button
            variant={lowStockOnly ? 'default' : 'outline'}
            size="sm"
            onClick={() => {
              setLowStockOnly((v) => !v);
              setPage(1);
            }}
          >
            <AlertTriangle className="h-4 w-4 mr-1" />
            {lowStockOnly ? 'All Items' : 'Low Stock Only'}
          </Button>
        </div>
        <Button onClick={() => setAddOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          Add Item
        </Button>
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="rounded-md border bg-white overflow-hidden">
          <table className="w-full text-sm">
            <tbody className="divide-y">
              {[...Array(5)].map((_, i) => (
                <tr key={i}>
                  {[...Array(9)].map((__, j) => (
                    <td key={j} className="px-4 py-3">
                      <div className="h-4 bg-zinc-100 rounded animate-pulse" />
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : filteredItems.length === 0 ? (
        <div className="rounded-md border bg-white p-12 text-center text-muted-foreground">
          <p className="font-medium">No inventory items found.</p>
          <p className="text-sm mt-1">Click &apos;Add Item&apos; to get started.</p>
        </div>
      ) : (
        <div className="rounded-md border bg-white overflow-x-auto">
          <table className="w-full text-sm min-w-[900px]">
            <thead className="bg-zinc-50 border-b">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-muted-foreground">Product</th>
                <th className="text-left px-4 py-3 font-medium text-muted-foreground">Category</th>
                <th className="text-left px-4 py-3 font-medium text-muted-foreground">Quantity</th>
                <th className="text-left px-4 py-3 font-medium text-muted-foreground">Unit</th>
                <th className="text-right px-4 py-3 font-medium text-muted-foreground">Cost (₹)</th>
                <th className="text-right px-4 py-3 font-medium text-muted-foreground">
                  Selling (₹)
                </th>
                <th className="text-left px-4 py-3 font-medium text-muted-foreground">Expiry</th>
                <th className="text-left px-4 py-3 font-medium text-muted-foreground">Location</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y">
              {filteredItems.map((item) => {
                const expiry = item.expiryDate ? formatExpiryDate(item.expiryDate) : null;
                return (
                  <tr key={item.id} className="hover:bg-zinc-50">
                    <td className="px-4 py-3 font-medium">
                      {item.productName ?? '—'}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {item.productCategory ?? '—'}
                    </td>
                    <td className="px-4 py-3">
                      <div className="space-y-1 min-w-[120px]">
                        <div className="flex items-center gap-2">
                          <span className="font-medium">{item.quantity}</span>
                          <StockBadge quantity={item.quantity} threshold={item.lowStockThreshold} />
                        </div>
                        <LowStockIndicator
                          quantity={item.quantity}
                          threshold={item.lowStockThreshold}
                        />
                      </div>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{item.unit}</td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      ₹{item.costPrice.toLocaleString('en-IN')}
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      ₹{item.sellingPrice.toLocaleString('en-IN')}
                    </td>
                    <td className="px-4 py-3">
                      {expiry ? (
                        <span className={cn(expiry.isNear && 'text-red-600 font-medium')}>
                          {expiry.formatted}
                        </span>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{item.location ?? '—'}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => setEditItem(item)}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-destructive hover:text-destructive"
                          onClick={() => setDeleteItem(item)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {data && data.pagination.pages > 1 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            Page {data.pagination.page} of {data.pagination.pages} ({data.pagination.total} items)
          </span>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => p - 1)}
              disabled={page === 1}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => p + 1)}
              disabled={page === data.pagination.pages}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      {/* Dialogs */}
      <AddInventoryDialog storeId={storeId} open={addOpen} onOpenChange={setAddOpen} />

      {editItem && (
        <EditInventoryDialog
          item={editItem}
          open={!!editItem}
          onOpenChange={(open) => !open && setEditItem(null)}
        />
      )}

      <Dialog open={!!deleteItem} onOpenChange={(open) => !open && setDeleteItem(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Inventory Item</DialogTitle>
            <DialogDescription>
              Are you sure you want to remove &quot;{deleteItem?.productName ?? 'this item'}&quot;
              from inventory? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteItem(null)}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={confirmDelete} disabled={isDeleting}>
              {isDeleting ? 'Deleting…' : 'Delete'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
