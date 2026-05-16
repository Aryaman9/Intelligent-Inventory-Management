import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

interface StockBadgeProps {
  quantity: number;
  threshold: number;
}

export function StockBadge({ quantity, threshold }: StockBadgeProps) {
  const getVariant = () => {
    if (quantity === 0) {
      return {
        label: 'Out of Stock',
        className: 'bg-red-100 text-red-800 border-red-200',
      };
    }
    if (quantity <= threshold) {
      return {
        label: 'Critical',
        className: 'bg-red-100 text-red-800 border-red-200',
      };
    }
    if (quantity <= threshold * 2) {
      return {
        label: 'Low Stock',
        className: 'bg-yellow-100 text-yellow-800 border-yellow-200',
      };
    }
    return {
      label: 'In Stock',
      className: 'bg-green-100 text-green-800 border-green-200',
    };
  };

  const { label, className } = getVariant();

  return (
    <Badge variant="outline" className={cn(className)}>
      {label}
    </Badge>
  );
}
