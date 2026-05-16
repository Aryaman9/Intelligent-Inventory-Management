import {
  PieChart,
  Pie,
  Cell,
  Legend,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

const COLORS = ['#2563eb', '#16a34a', '#d97706', '#7c3aed', '#dc2626'];

interface PaymentBreakdownProps {
  breakdown: Record<string, number>;
}

export function PaymentBreakdown({ breakdown }: PaymentBreakdownProps) {
  const chartData = Object.entries(breakdown).map(([name, value]) => ({ name, value }));

  if (chartData.length === 0) {
    return (
      <div className="flex items-center justify-center h-[250px] text-sm text-muted-foreground">
        No payment data for selected period
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={250}>
      <PieChart>
        <Pie
          data={chartData}
          dataKey="value"
          nameKey="name"
          cx="50%"
          cy="50%"
          outerRadius={80}
          label
        >
          {chartData.map((_, index) => (
            <Cell key={index} fill={COLORS[index % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip formatter={(value: number) => [`₹${value}`, '']} />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
}
