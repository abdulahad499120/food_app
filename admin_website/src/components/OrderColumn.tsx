import React from 'react';
import { OrderStatus } from '../models/types';
import type { Order } from '../models/types';
import { OrderCard } from './OrderCard';

interface Props {
  title: string;
  orders: Order[];
  onUpdateStatus: (orderId: string, newStatus: OrderStatus, additionalUpdates?: any) => void;
}

export const OrderColumn: React.FC<Props> = ({ title, orders, onUpdateStatus }) => {
  return (
    <div className="flex flex-col bg-gray-100 rounded-2xl flex-1 overflow-hidden h-full border border-gray-200">
      <div className="bg-gray-200 px-4 py-3 flex justify-between items-center border-b border-gray-300">
        <h2 className="font-extrabold text-gray-700 tracking-wide">{title}</h2>
        <span className="bg-white text-gray-800 font-bold px-3 py-1 rounded-full text-sm shadow-sm">
          {orders.length}
        </span>
      </div>
      
      <div className="p-4 flex-1 overflow-y-auto space-y-4">
        {orders.length === 0 ? (
          <div className="h-full flex items-center justify-center text-gray-400 font-medium italic">
            No orders
          </div>
        ) : (
          orders.map(order => (
            <OrderCard key={order.orderId} order={order} onUpdateStatus={onUpdateStatus} />
          ))
        )}
      </div>
    </div>
  );
};
