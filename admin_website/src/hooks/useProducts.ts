import { useState, useEffect } from 'react';
import { collection, onSnapshot, doc, updateDoc, addDoc, deleteDoc } from 'firebase/firestore';
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

  const addProduct = async (product: Omit<Product, 'id'>) => {
    try {
      const promise = addDoc(collection(db, "products"), product);
      await toast.promise(promise, {
        loading: 'Adding product...',
        success: 'Product added successfully!',
        error: 'Failed to add product'
      });
    } catch (error) {
      console.error(error);
    }
  };

  const updateProduct = async (product: Product) => {
    try {
      const { id, ...data } = product;
      const promise = updateDoc(doc(db, "products", id), data);
      await toast.promise(promise, {
        loading: 'Updating product...',
        success: 'Product updated successfully!',
        error: 'Failed to update product'
      });
    } catch (error) {
      console.error(error);
    }
  };

  const deleteProduct = async (productId: string) => {
    try {
      const promise = deleteDoc(doc(db, "products", productId));
      await toast.promise(promise, {
        loading: 'Deleting product...',
        success: 'Product deleted!',
        error: 'Failed to delete product'
      });
    } catch (error) {
      console.error(error);
    }
  };

  return { products, loading, updateProductAvailability, addProduct, updateProduct, deleteProduct };
};
