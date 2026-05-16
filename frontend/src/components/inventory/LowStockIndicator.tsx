interface LowStockIndicatorProps {
  quantity: number;
  threshold: number;
}

export function LowStockIndicator({ quantity, threshold }: LowStockIndicatorProps) {
  const maxQuantity = threshold * 2;
  const widthPercent = Math.min(quantity / (maxQuantity || 1), 1) * 100;

  const getColor = () => {
    if (quantity === 0 || quantity <= threshold) return '#ef4444'; // red-500
    if (quantity <= threshold * 2) return '#eab308'; // yellow-500
    return '#22c55e'; // green-500
  };

  return (
    <div className="w-full h-1.5 bg-zinc-100 rounded-full overflow-hidden">
      <div
        className="h-full rounded-full transition-all duration-300"
        style={{ width: `${widthPercent}%`, backgroundColor: getColor() }}
      />
    </div>
  );
}
