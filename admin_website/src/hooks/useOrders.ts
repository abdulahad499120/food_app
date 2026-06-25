import { useState, useEffect } from 'react';
import { collection, query, where, orderBy, limit, onSnapshot, doc, updateDoc } from 'firebase/firestore';
import { db, ACTIVE_BRANCH_ID } from '../firebase';
import { OrderStatus } from '../models/types';
import type { Order } from '../models/types';
import toast from 'react-hot-toast';

export const useOrders = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const ordersRef = collection(db, 'orders');
    // Using order by timestamp and limit to 100 to avoid massive reads.
    // The client will filter out DELIVERED and CANCELLED
    const q = query(
      ordersRef,
      where("branchId", "==", ACTIVE_BRANCH_ID),
      orderBy("timestamp", "desc"),
      limit(100)
    );

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const fetchedOrders: Order[] = [];
      snapshot.forEach((docSnap) => {
        const data = docSnap.data();
        if (data.orderStatus !== OrderStatus.DELIVERED && data.orderStatus !== OrderStatus.CANCELLED) {
          fetchedOrders.push({
            orderId: docSnap.id,
            ...data,
            timestamp: data.timestamp?.toMillis ? data.timestamp.toMillis() : (data.timestamp || Date.now())
          } as Order);
        }
      });
      setOrders(fetchedOrders);
      setLoading(false);
    }, (error) => {
      console.error("Error fetching orders:", error);
      toast.error("Failed to connect to order stream");
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const updateOrderStatus = async (orderId: string, newStatus: OrderStatus, additionalUpdates: any = {}) => {
    // Optimistic UI Update is tricky with onSnapshot, but we can do a UI toast wrapper
    const promise = updateDoc(doc(db, "orders", orderId), {
      orderStatus: newStatus,
      ...additionalUpdates
    });

    toast.promise(promise, {
      loading: 'Updating status...',
      success: 'Status updated successfully',
      error: 'Failed to update status'
    });

    return promise;
  };

  return { orders, loading, updateOrderStatus };
};
