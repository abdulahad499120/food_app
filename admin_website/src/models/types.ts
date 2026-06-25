export const OrderStatus = {
  GRACE_PERIOD: "GRACE_PERIOD",
  PENDING: "PENDING",
  PREPARING: "PREPARING",
  READY_FOR_RIDER: "READY_FOR_RIDER",
  RIDER_ASSIGNED: "RIDER_ASSIGNED",
  OUT_FOR_DELIVERY: "OUT_FOR_DELIVERY",
  DELIVERED: "DELIVERED",
  CANCELLED: "CANCELLED"
} as const;
export type OrderStatus = typeof OrderStatus[keyof typeof OrderStatus];

export const FulfillmentMode = {
  DELIVERY: "DELIVERY",
  PICKUP: "PICKUP"
} as const;
export type FulfillmentMode = typeof FulfillmentMode[keyof typeof FulfillmentMode];

export interface Product {
  id: string;
  name: string;
  price: number;
  image: string;
  isAvailable: boolean;
  category_name: string;
}

export interface CartItem {
  product: Product;
  quantity: number;
  size: string;
  sweetness: number;
}

export interface Order {
  orderId: string;
  customerName: string;
  items: CartItem[];
  totalAmount: number;
  orderStatus: OrderStatus;
  fulfillmentMode: FulfillmentMode;
  riderId?: string;
  riderName?: string;
  riderVehicle?: string;
  riderPhone?: string;
  generalSector?: string;
  itemSummary?: string;
  branchLocation?: { latitude: number; longitude: number };
  driverLocation?: { latitude: number; longitude: number };
  deliveryLocation?: { latitude: number; longitude: number };
  deliveryAddress?: {
    streetAddress: string;
    city: string;
    phoneNumber: string;
  };
  totalEtaMinutes?: number;
  timestamp?: number; // In a real app, this is a Date or Firebase Timestamp
}
