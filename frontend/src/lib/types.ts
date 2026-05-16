export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  error?: string;
  errorCode?: string;
}

export interface PaginatedData<T> {
  items: T[];
  pagination: {
    total: number;
    page: number;
    limit: number;
    pages: number;
  };
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: string;
  subscriptionPlan: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface Store {
  id: string;
  name: string;
  type: string;
  gstin?: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
  latitude?: number;
  longitude?: number;
  phone?: string;
  email?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StoreStats {
  [key: string]: number;
}

export interface Product {
  id: string;
  name: string;
  category: string;
  brand?: string;
  barcode?: string;
  variants?: ProductVariant[];
  attributes?: ProductAttributes;
  tags?: string[];
  active: boolean;
  createdAt: string;
}

export interface ProductVariant {
  size: string;
  unit: string;
  mrp: number;
}

export interface ProductAttributes {
  perishable?: boolean;
  shelfLifeDays?: number;
  requiresPrescription?: boolean;
  seasonal?: boolean;
}

// Inventory
export interface Inventory {
  id: string;
  storeId: string;
  productId: string;
  productName?: string;
  productCategory?: string;
  quantity: number;
  unit: string;
  lowStockThreshold: number;
  reorderQuantity?: number;
  costPrice: number;
  sellingPrice: number;
  mrp?: number;
  batchNumber?: string;
  expiryDate?: string;
  lastRestockedAt?: string;
  lastSoldAt?: string;
  location?: string;
  isActive: boolean;
}

export interface InventoryStats {
  totalItems: number;
  totalValue: number;
  lowStockCount: number;
  expiringCount: number;
}

export interface AddInventoryRequest {
  storeId: string;
  productId: string;
  quantity: number;
  costPrice: number;
  sellingPrice: number;
  lowStockThreshold?: number;
  reorderQuantity?: number;
  unit?: string;
  mrp?: number;
  batchNumber?: string;
  expiryDate?: string;
  location?: string;
}

// Transactions
export interface Transaction {
  id: string;
  storeId: string;
  inventoryId: string;
  type: 'SALE' | 'PURCHASE' | 'RETURN' | 'ADJUSTMENT';
  quantity: number;
  pricePerUnit: number;
  totalAmount: number;
  paymentMethod?: string;
  customerName?: string;
  customerPhone?: string;
  invoiceNumber?: string;
  notes?: string;
  createdAt: string;
  productName?: string;
}

export interface SaleRequest {
  inventoryId: string;
  quantity: number;
  pricePerUnit: number;
  paymentMethod: string;
  customerName?: string;
  customerPhone?: string;
  notes?: string;
}

export interface PurchaseRequest {
  inventoryId: string;
  quantity: number;
  pricePerUnit: number;
  notes?: string;
}

export interface SaleResponse {
  transaction: {
    id: string;
    type: string;
    quantity: string;
    totalAmount: string;
    invoiceNumber: string;
  };
  inventoryQuantity: string;
  productName: string;
}

// Alerts
export interface LowStockAlert {
  inventoryId: string;
  quantity: number;
  lowStockThreshold: number;
  shortage: number;
  product: { id: string; name: string };
  store: { id: string; name: string };
}

export interface ExpiryAlert {
  inventoryId: string;
  expiryDate: string;
  daysUntilExpiry: number;
  product: { id: string; name: string };
  store: { id: string; name: string };
}

export interface AlertsData {
  lowStockAlerts: LowStockAlert[];
  expiryAlerts: ExpiryAlert[];
  summary: { lowStockCount: number; expiringSoonCount: number };
}

// Analytics
export interface TransactionStats {
  totalTransactions: number;
  totalSalesCount: number;
  totalPurchasesCount: number;
  totalSalesAmount: string;
  totalRevenue: string;
  totalProfit: string;
  profitMargin: string;
  paymentMethodBreakdown: Record<string, number>;
  dailyRevenue: { date: string; revenue: string }[];
}
