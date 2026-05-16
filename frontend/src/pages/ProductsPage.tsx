import React, { useState } from 'react';
import { Plus, Search, ChevronDown, ChevronUp, Trash2 } from 'lucide-react';
import { useProducts, useCategories, useDeleteProduct, useUpdateProduct } from '@/hooks/useProducts';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from '@/components/ui/dialog';
import { CreateProductDialog } from '@/components/products/CreateProductDialog';
import type { Product } from '@/lib/types';

export function ProductsPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [category, setCategory] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [deleteProduct, setDeleteProduct] = useState<Product | null>(null);

  const { data, isLoading } = useProducts(page, search, category);
  const { data: categories } = useCategories();
  const { mutateAsync: deleteFn, isPending: isDeleting } = useDeleteProduct();
  const { mutateAsync: updateFn } = useUpdateProduct();
  const { toast } = useToast();

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setSearch(searchInput);
    setPage(1);
  }

  async function confirmDelete() {
    if (!deleteProduct) return;
    try {
      await deleteFn(deleteProduct.id);
      toast({ title: 'Product deleted' });
      setDeleteProduct(null);
    } catch {
      toast({ variant: 'destructive', title: 'Error', description: 'Failed to delete product' });
    }
  }

  async function toggleActive(product: Product) {
    try {
      await updateFn({ id: product.id, active: !product.active });
    } catch {
      toast({ variant: 'destructive', title: 'Error', description: 'Failed to update product' });
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Product Catalog</h1>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          Add Product
        </Button>
      </div>

      <div className="flex flex-wrap gap-2">
        <form onSubmit={handleSearch} className="flex gap-2">
          <Input
            placeholder="Search products…"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-48"
          />
          <Button type="submit" variant="outline" size="icon">
            <Search className="h-4 w-4" />
          </Button>
        </form>
        <Select
          value={category}
          onValueChange={(v) => {
            setCategory(v === 'ALL' ? '' : v);
            setPage(1);
          }}
        >
          <SelectTrigger className="w-44">
            <SelectValue placeholder="All categories" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All categories</SelectItem>
            {categories?.map((c) => (
              <SelectItem key={c} value={c}>
                {c.charAt(0) + c.slice(1).toLowerCase().replace('_', ' ')}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="h-14 bg-zinc-100 rounded animate-pulse" />
          ))}
        </div>
      ) : !data?.items.length ? (
        <div className="text-center py-16 text-muted-foreground">
          <p className="text-lg">No products yet</p>
          <p className="text-sm mt-1">Add your first product to the catalog</p>
        </div>
      ) : (
        <>
          <div className="rounded-md border bg-white overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-zinc-50 border-b">
                <tr>
                  <th className="w-8 px-2 py-3" />
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Name</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Category</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground hidden md:table-cell">Brand</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground hidden lg:table-cell">Barcode</th>
                  <th className="text-left px-4 py-3 font-medium text-muted-foreground">Status</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y">
                {data.items.map((product) => (
                  <React.Fragment key={product.id}>
                    <tr className="hover:bg-zinc-50">
                      <td className="px-2 py-3">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-6 w-6"
                          onClick={() =>
                            setExpandedId(expandedId === product.id ? null : product.id)
                          }
                        >
                          {expandedId === product.id ? (
                            <ChevronUp className="h-3 w-3" />
                          ) : (
                            <ChevronDown className="h-3 w-3" />
                          )}
                        </Button>
                      </td>
                      <td className="px-4 py-3 font-medium">{product.name}</td>
                      <td className="px-4 py-3 text-muted-foreground capitalize">
                        {product.category.toLowerCase().replace('_', ' ')}
                      </td>
                      <td className="px-4 py-3 text-muted-foreground hidden md:table-cell">
                        {product.brand ?? '—'}
                      </td>
                      <td className="px-4 py-3 text-muted-foreground font-mono text-xs hidden lg:table-cell">
                        {product.barcode ?? '—'}
                      </td>
                      <td className="px-4 py-3">
                        <button
                          onClick={() => toggleActive(product)}
                          className="focus:outline-none"
                        >
                          <Badge variant={product.active ? 'default' : 'secondary'}>
                            {product.active ? 'Active' : 'Inactive'}
                          </Badge>
                        </button>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            className="text-destructive hover:text-destructive"
                            onClick={() => setDeleteProduct(product)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                    {expandedId === product.id && (
                      <tr className="bg-zinc-50">
                        <td colSpan={7} className="px-6 py-3">
                          <div className="space-y-2 text-sm">
                            {product.variants?.length ? (
                              <div>
                                <p className="font-medium text-muted-foreground mb-1">Variants</p>
                                <div className="flex flex-wrap gap-2">
                                  {product.variants.map((v, i) => (
                                    <span
                                      key={i}
                                      className="bg-white border rounded px-2 py-1 text-xs"
                                    >
                                      {v.size} {v.unit} — ₹{v.mrp}
                                    </span>
                                  ))}
                                </div>
                              </div>
                            ) : null}
                            {product.tags?.length ? (
                              <div className="flex flex-wrap gap-1">
                                {product.tags.map((tag) => (
                                  <Badge key={tag} variant="outline" className="text-xs">
                                    {tag}
                                  </Badge>
                                ))}
                              </div>
                            ) : null}
                            {!product.variants?.length && !product.tags?.length && (
                              <p className="text-muted-foreground text-xs">No additional details</p>
                            )}
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>

          {data.pagination.pages > 1 && (
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>
                Page {data.pagination.page} of {data.pagination.pages} ({data.pagination.total} total)
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

      <CreateProductDialog open={createOpen} onOpenChange={setCreateOpen} />

      <Dialog open={!!deleteProduct} onOpenChange={(open) => !open && setDeleteProduct(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Product</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete "{deleteProduct?.name}"?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteProduct(null)}>
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
