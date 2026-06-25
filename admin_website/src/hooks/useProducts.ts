import { useState, useEffect } from 'react';
import { collection, onSnapshot, doc, updateDoc } from 'firebase/firestore';
import { db } from '../firebase';
import type { Product } from '../models/types';
import toast from 'react-hot-toast';

export const useProducts = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const productsRef = collection(db, 'products');
    const unsubscribe = onSnapshot(productsRef, (snapshot) => {
      const fetchedProducts: Product[] = [];
      snapshot.forEach((docSnap) => {
        fetchedProducts.push({
          id: docSnap.id,
          ...docSnap.data()
        } as Product);
      });
      setProducts(fetchedProducts);
      setLoading(false);
    }, (error) => {
      console.error("Error fetching products:", error);
      toast.error("Failed to connect to products stream");
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const updateProductAvailability = async (productId: string, isAvailable: boolean) => {
    // Optimistic Update
    setProducts(prev => prev.map(p => p.id === productId ? { ...p, isAvailable } : p));
    
    try {
      const promise = updateDoc(doc(db, "products", productId), {
        isAvailable
      });
      
      await toast.promise(promise, {
        loading: 'Saving...',
        success: 'Inventory updated',
        error: 'Failed to update inventory'
      });
    } catch (error) {
      // Revert optimistic update
      setProducts(prev => prev.map(p => p.id === productId ? { ...p, isAvailable: !isAvailable } : p));
    }
  };

  return { products, loading, updateProductAvailability };
};
