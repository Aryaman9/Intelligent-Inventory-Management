import { NavLink, useParams } from 'react-router-dom';
import {
  LayoutDashboard,
  Store,
  Package,
  Bell,
  ShoppingCart,
  PackagePlus,
  ArrowRightLeft,
  BarChart3,
  Boxes,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAlerts } from '@/hooks/useInventory';

const topNavItems = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, exact: true },
  { to: '/stores', label: 'Stores', icon: Store },
  { to: '/products', label: 'Products', icon: Package },
];

export function Sidebar() {
  const { id: storeId } = useParams<{ id: string }>();
  const { data: alertsData } = useAlerts();
  const alertCount =
    (alertsData?.summary.lowStockCount ?? 0) + (alertsData?.summary.expiringSoonCount ?? 0);

  return (
    <aside className="hidden lg:flex flex-col w-64 bg-zinc-900 text-zinc-100 min-h-screen shrink-0">
      <div className="flex items-center h-16 px-6 border-b border-zinc-800">
        <span className="text-lg font-semibold tracking-tight">Inventra</span>
      </div>
      <nav className="flex-1 px-3 py-4 space-y-1">
        {topNavItems.map(({ to, label, icon: Icon, exact }) => (
          <NavLink
            key={to}
            to={to}
            end={exact}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors',
                isActive
                  ? 'bg-zinc-700 text-white'
                  : 'text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100'
              )
            }
          >
            <Icon className="h-4 w-4" />
            <span>{label}</span>
          </NavLink>
        ))}

        <NavLink
          to="/alerts"
          className={({ isActive }) =>
            cn(
              'flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors',
              isActive
                ? 'bg-zinc-700 text-white'
                : 'text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100'
            )
          }
        >
          <Bell className="h-4 w-4" />
          <span>Alerts</span>
          {alertCount > 0 && (
            <span className="ml-auto bg-red-500 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full min-w-[18px] text-center">
              {alertCount}
            </span>
          )}
        </NavLink>

        {storeId && (
          <>
            <div className="pt-3 pb-1 px-3">
              <p className="text-[10px] uppercase tracking-widest text-zinc-500 font-medium">
                Store Actions
              </p>
            </div>
            <NavLink
              to={`/stores/${storeId}/inventory`}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors',
                  isActive
                    ? 'bg-zinc-700 text-white'
                    : 'text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100'
                )
              }
            >
              <Boxes className="h-4 w-4" />
              <span>Inventory</span>
            </NavLink>
            <NavLink
              to={`/stores/${storeId}/sale`}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors',
                  isActive
                    ? 'bg-zinc-700 text-white'
                    : 'text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100'
                )
              }
            >
              <ShoppingCart className="h-4 w-4" />
              <span>Record Sale</span>
            </NavLink>
            <NavLink
              to={`/stores/${storeId}/purchase`}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors',
                  isActive
                    ? 'bg-zinc-700 text-white'
                    : 'text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100'
                )
              }
            >
              <PackagePlus className="h-4 w-4" />
              <span>Restock</span>
            </NavLink>
            <NavLink
              to={`/stores/${storeId}/transactions`}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors',
                  isActive
                    ? 'bg-zinc-700 text-white'
                    : 'text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100'
                )
              }
            >
              <ArrowRightLeft className="h-4 w-4" />
              <span>Transactions</span>
            </NavLink>
            <NavLink
              to={`/stores/${storeId}/analytics`}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors',
                  isActive
                    ? 'bg-zinc-700 text-white'
                    : 'text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100'
                )
              }
            >
              <BarChart3 className="h-4 w-4" />
              <span>Analytics</span>
            </NavLink>
          </>
        )}
      </nav>
    </aside>
  );
}
