import { OrderStatus, FulfillmentMode } from "../models/types";
import type { Order, Product } from "../models/types";

export const MOCK_PRODUCTS: Product[] = [
  { id: "p1", name: "Classic Burger", price: 8.99, image: "", isAvailable: true, category_name: "Burgers" },
  { id: "p2", name: "Cheese Fries", price: 4.99, image: "", isAvailable: true, category_name: "Sides" },
  { id: "p3", name: "Strawberry Shake", price: 5.99, image: "", isAvailable: false, category_name: "Drinks" }
];

export const MOCK_ORDERS: Order[] = [
  {
    orderId: "ord_001",
    customerName: "Alice Smith",
    items: [
      { product: MOCK_PRODUCTS[0], quantity: 2, size: "Regular", sweetness: 2 },
      { product: MOCK_PRODUCTS[1], quantity: 1, size: "Regular", sweetness: 2 }
    ],
    totalAmount: 22.97,
    orderStatus: OrderStatus.PENDING,
    fulfillmentMode: FulfillmentMode.DELIVERY,
    timestamp: Date.now() - 1000 * 60 * 5, // 5 mins ago
  },
  {
    orderId: "ord_002",
    customerName: "Bob Johnson",
    items: [
      { product: MOCK_PRODUCTS[2], quantity: 1, size: "Large", sweetness: 2 }
    ],
    totalAmount: 5.99,
    orderStatus: OrderStatus.PREPARING,
    fulfillmentMode: FulfillmentMode.PICKUP,
    timestamp: Date.now() - 1000 * 60 * 15, // 15 mins ago
  },
  {
    orderId: "ord_003",
    customerName: "Charlie Brown",
    items: [
      { product: MOCK_PRODUCTS[0], quantity: 1, size: "Regular", sweetness: 2 }
    ],
    totalAmount: 8.99,
    orderStatus: OrderStatus.READY_FOR_RIDER,
    fulfillmentMode: FulfillmentMode.DELIVERY,
    timestamp: Date.now() - 1000 * 60 * 25,
  }
];
