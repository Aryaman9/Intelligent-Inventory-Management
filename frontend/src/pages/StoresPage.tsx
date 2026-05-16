import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, Pencil, Trash2, Eye } from 'lucide-react';
import { useStores, useDeleteStore } from '@/hooks/useStores';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from '@/components/ui/dialog';
import { CreateStoreDialog } from '@/components/stores/CreateStoreDialog';
import { EditStoreDialog } from '@/components/stores/EditStoreDialog';
import type { Store } from '@/lib/types';

export function StoresPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [editStore, setEditStore] = useState<Store | null>(null);
  const [deleteStore, setDeleteStore] = useState<Store | null>(null);

  const { data, isLoading } = useStores(page, search);
  const { mutateAsync: deleteStoreFn, isPending: isDeleting } = useDeleteStore();
  const { toast } = useToast();
  const navigate = useNavigate();

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setSearch(searchInput);
    setPage(1);
  }

  async function confirmDelete() {
    if (!deleteStore) return;
    try {
      await deleteStoreFn(deleteStore.id);
      toast({ title: 'Store deleted' });
      setDeleteStore(null);
    } catch {
      toast({ variant: 'destructive', title: 'Error', description: 'Failed to delete store' });
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">My Stores</h1>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          Add Store
        </Button>
      </div>

      <form onSubmit={handleSearch} className="flex gap-2 max-w-sm">
        <Input
          placeholder="Search stores…"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
        />
        <Button type="submit" variant="outline" size="icon">
          <Search className="h-4 w-4" />
        </Button>
      </form>

      {isLoading ? (
        <div className="space-y-2">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-12 bg-zinc-100 rounded animate-pulse" />
          ))}
        </div>
      ) : !data?.items.length ? (
        <div className="text-center py-16 text-muted-foreground">
          <p className="text-lg">No stores yet</p>
          <p className="text-sm mt-1">Create your first store to get started</p>
        </div>
      ) : (
        <>
          <div className="rounded-md border bg-white overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-zinc-50 border-b">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Name</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Type</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">City</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Status</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y">
                {data.items.map((store) => (
                  <tr key={store.id} className="hover:bg-zinc-50">
                    <td className="px-4 py-3 font-medium">{store.name}</td>
                    <td className="px-4 py-3 text-muted-foreground capitalize">
                      {store.type.toLowerCase()}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{store.city}</td>
                    <td className="px-4 py-3">
                      <Badge variant={store.isActive ? 'default' : 'secondary'}>
                        {store.isActive ? 'Active' : 'Inactive'}
                      </Badge>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => navigate(`/stores/${store.id}`)}
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => setEditStore(store)}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-destructive hover:text-destructive"
                          onClick={() => setDeleteStore(store)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {data.pagination.pages > 1 && (
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>
                Page {data.pagination.page} of {data.pagination.pages}
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
        </>
      )}

      <CreateStoreDialog open={createOpen} onOpenChange={setCreateOpen} />

      {editStore && (
        <EditStoreDialog
          store={editStore}
          open={!!editStore}
          onOpenChange={(open) => !open && setEditStore(null)}
        />
      )}

      <Dialog open={!!deleteStore} onOpenChange={(open) => !open && setDeleteStore(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Store</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete "{deleteStore?.name}"? This action can be undone by
              an admin.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteStore(null)}>
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
