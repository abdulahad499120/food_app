import React, { useEffect, useRef } from 'react';
import { OrderStatus } from '../models/types';
import type { Order } from '../models/types';
import { OrderColumn } from './OrderColumn';

interface Props {
  orders: Order[];
  onUpdateStatus: (orderId: string, newStatus: OrderStatus, additionalUpdates?: any) => void;
}

export const KanbanBoard: React.FC<Props> = ({ orders, onUpdateStatus }) => {
  const pendingOrders = orders.filter(o => o.orderStatus === OrderStatus.PENDING);
  const preparingOrders = orders.filter(o => o.orderStatus === OrderStatus.PREPARING);
  const waitingOrders = orders.filter(o => ([OrderStatus.READY_FOR_RIDER, OrderStatus.RIDER_ASSIGNED] as OrderStatus[]).includes(o.orderStatus));
  const activeOrders = orders.filter(o => o.orderStatus === OrderStatus.OUT_FOR_DELIVERY);

  // Daily Stats Calculations
  const totalOrders = orders.length;
  const totalRevenue = orders.reduce((acc, order) => acc + (order.totalAmount || 0), 0);
  const activeDeliveries = orders.filter(o => o.orderStatus === OrderStatus.OUT_FOR_DELIVERY).length;

  // Audio Alert System for PENDING orders
  const audioRef = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    audioRef.current = new Audio('/new_order_chime.mp3');
    audioRef.current.loop = true;
  }, []);

  useEffect(() => {
    if (pendingOrders.length > 0) {
      audioRef.current?.play().catch(e => console.log("Audio play blocked by browser. Click anywhere to enable.", e));
    } else {
      if (audioRef.current) {
        audioRef.current.pause();
        audioRef.current.currentTime = 0; // Reset to beginning for next alarm
      }
  }, [pendingOrders.length]);

  // Auto-transition orders from KITCHEN PREP to READY_FOR_HANDOFF when prepTime expires
  useEffect(() => {
    const interval = setInterval(() => {
      const now = Date.now();
      preparingOrders.forEach(order => {
        const prepStart = (order as any).prepStartTime || order.timestamp || now;
        const pTime = (order as any).prepTime || 15; // default 15 mins if missing
        
        const expectedReadyTime = prepStart + (pTime * 60000);
        if (now >= expectedReadyTime) {
          onUpdateStatus(order.orderId, OrderStatus.READY_FOR_RIDER);
        }
      });
    }, 5000); // check every 5 seconds
    return () => clearInterval(interval);
  }, [preparingOrders, onUpdateStatus]);

  return (
    <div className="flex flex-col h-full w-full p-6 bg-gray-50">
      {/* Daily Stats Header */}
      <div className="flex flex-wrap gap-4 mb-6">
        <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex-1 min-w-[200px]">
          <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-1">Total Orders Today</h3>
          <p className="text-3xl font-black text-gray-900">{totalOrders}</p>
        </div>
        <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex-1 min-w-[200px]">
          <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-1">Total Revenue</h3>
          <p className="text-3xl font-black text-green-600">Rs. {totalRevenue.toFixed(0)}</p>
        </div>
        <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex-1 min-w-[200px]">
          <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-1">Active Deliveries</h3>
          <p className="text-3xl font-black text-blue-600">{activeDeliveries}</p>
        </div>
      </div>

      <div className="flex gap-6 flex-1 overflow-hidden">
        <OrderColumn title="NEW ORDERS" orders={pendingOrders} onUpdateStatus={onUpdateStatus} />
        <OrderColumn title="KITCHEN PREP" orders={preparingOrders} onUpdateStatus={onUpdateStatus} />
        <OrderColumn title="WAITING FOR HANDOFF" orders={waitingOrders} onUpdateStatus={onUpdateStatus} />
        <OrderColumn title="OUT FOR DELIVERY" orders={activeOrders} onUpdateStatus={onUpdateStatus} />
      </div>
    </div>
  );
};
