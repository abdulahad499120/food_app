import React from 'react';
import { OrderStatus, FulfillmentMode } from '../models/types';
import type { Order } from '../models/types';
import { Clock, Car, ShoppingBag, CheckCircle } from 'lucide-react';

interface Props {
  order: Order;
  onUpdateStatus: (orderId: string, newStatus: OrderStatus, additionalUpdates?: any) => void;
}

const calculateDistance = (lat1: number, lon1: number, lat2: number, lon2: number) => {
  const R = 6371; // km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
};

export const OrderCard: React.FC<Props> = ({ order, onUpdateStatus }) => {
  const isDelivery = order.fulfillmentMode === FulfillmentMode.DELIVERY;
  const timeElapsed = Math.floor((Date.now() - (order.timestamp || Date.now())) / 60000);

  const getAction = () => {
    switch (order.orderStatus) {
      case OrderStatus.PENDING:
        return {
          label: "Accept & Prepare",
          color: "bg-green-600 hover:bg-green-700",
          next: OrderStatus.PREPARING
        };
      case OrderStatus.PREPARING:
        return isDelivery ? {
          label: "Ready for Rider",
          color: "bg-pink-600 hover:bg-pink-700",
          next: OrderStatus.READY_FOR_RIDER
        } : {
          label: "Mark Ready for Customer",
          color: "bg-orange-600 hover:bg-orange-700",
          next: OrderStatus.OUT_FOR_DELIVERY
        };
      case OrderStatus.OUT_FOR_DELIVERY:
        return isDelivery ? null : {
          label: "Order Handed Over",
          color: "bg-gray-800 hover:bg-gray-900",
          next: OrderStatus.DELIVERED
        };
      default:
        return null;
    }
  };

  const action = getAction();

  let isRiderOutside = false;
  if (order.orderStatus === OrderStatus.READY_FOR_RIDER && order.driverLocation && order.branchLocation) {
     const distKm = calculateDistance(order.branchLocation.latitude, order.branchLocation.longitude, order.driverLocation.latitude, order.driverLocation.longitude);
     if (distKm < 0.1) { // 100 meters
       isRiderOutside = true;
     }
  }

  return (
    <div className={`p-4 rounded-xl border-l-8 shadow-sm bg-white flex flex-col gap-3 transition-all duration-200 hover:-translate-y-1 hover:shadow-lg ${
      isDelivery ? "border-pink-500" : "border-orange-500"
    } ${order.orderStatus === OrderStatus.PENDING ? "animate-pulse" : ""}`}>
      
      <div className="flex justify-between items-start">
        <div>
          <h3 className="font-bold text-lg text-gray-900">#{order.orderId.slice(-6).toUpperCase()}</h3>
          <p className="text-gray-600 font-medium">{order.customerName}</p>
        </div>
        <div className={`px-3 py-1 rounded-full flex items-center gap-1 font-bold text-sm ${
          isDelivery ? "bg-pink-100 text-pink-700" : "bg-orange-100 text-orange-700"
        }`}>
          {isDelivery ? <Car size={16}/> : <ShoppingBag size={16}/>}
          {isDelivery ? "DELIVERY" : "PICKUP"}
        </div>
      </div>

      <div className="flex items-center gap-1 text-gray-500 text-sm font-medium">
        <Clock size={14} />
        {timeElapsed} min ago
      </div>

      <div className="bg-gray-50 p-3 rounded-lg text-sm border border-gray-100">
        <ul className="space-y-2">
          {order.items.map((item, idx) => (
            <li key={idx} className="flex justify-between font-medium">
              <span><span className="text-gray-400 mr-2">{item.quantity}x</span> {item.product.name}</span>
            </li>
          ))}
        </ul>
      </div>

      {order.orderStatus === OrderStatus.READY_FOR_RIDER && (
        <div className={`flex items-center justify-center gap-2 p-3 rounded-lg font-bold ${isRiderOutside ? 'bg-red-100 text-red-600 animate-pulse' : 'bg-pink-50 text-pink-700'}`}>
          {!isRiderOutside && <div className="w-4 h-4 border-2 border-pink-600 border-t-transparent rounded-full animate-spin"></div>}
          {isRiderOutside ? "RIDER OUTSIDE!" : "Searching for Rider..."}
        </div>
      )}

      {order.orderStatus === OrderStatus.RIDER_ASSIGNED && (
        <div className="p-3 bg-green-50 text-green-800 rounded-lg text-sm">
          <p className="font-bold">Rider Assigned: {order.riderName}</p>
          <p>{order.riderVehicle}</p>
        </div>
      )}

      {action && (
        <button
          onClick={() => {
            if (action.next === OrderStatus.PREPARING) {
              const prepTimeStr = window.prompt("Enter Prep Time (minutes):", "15");
              if (!prepTimeStr) return; // User cancelled
              const prepTime = parseInt(prepTimeStr, 10) || 15;
              let driveTime = 15; // default fallback
              if (order.branchLocation && order.deliveryLocation) {
                 const distKm = calculateDistance(order.branchLocation.latitude, order.branchLocation.longitude, order.deliveryLocation.latitude, order.deliveryLocation.longitude);
                 driveTime = Math.ceil((distKm / 30) * 60); // Distance / 30km/h * 60 mins
              }
              const totalEtaMinutes = prepTime + driveTime;
              const prepStartTime = Date.now();
              onUpdateStatus(order.orderId, action.next, { totalEtaMinutes, prepTime, prepStartTime });
            } else {
              onUpdateStatus(order.orderId, action.next);
            }
          }}
          className={`w-full py-4 rounded-xl text-white font-bold text-lg shadow-md flex items-center justify-center gap-2 active:scale-95 transition-transform duration-150 ${action.color}`}
        >
          <CheckCircle size={20} />
          {action.label}
        </button>
      )}
    </div>
  );
};
