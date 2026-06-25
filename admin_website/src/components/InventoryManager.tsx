import React from 'react';
import type { Product } from '../models/types';

interface Props {
  products: Product[];
  onToggleAvailability: (productId: string, currentStatus: boolean) => void;
}

export const InventoryManager: React.FC<Props> = ({ products, onToggleAvailability }) => {
  return (
    <div className="p-6 h-full w-full bg-gray-50 flex flex-col">
      <h1 className="text-3xl font-extrabold text-gray-900 mb-6">Live Inventory Kill-Switch</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 overflow-y-auto pb-8">
        {products.map(product => (
          <div key={product.id} className={`p-4 rounded-xl border flex items-center justify-between shadow-sm transition-colors ${
            product.isAvailable ? "bg-white border-gray-200" : "bg-red-50 border-red-200"
          }`}>
            <div>
              <p className="text-xs text-gray-500 font-bold uppercase tracking-wider mb-1">{product.category_name}</p>
              <h3 className={`font-bold text-lg ${product.isAvailable ? "text-gray-900" : "text-gray-400 line-through"}`}>
                {product.name}
              </h3>
            </div>
            
            <button
              onClick={() => onToggleAvailability(product.id, product.isAvailable)}
              className={`relative inline-flex h-8 w-14 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-pink-500 ${
                product.isAvailable ? 'bg-pink-600' : 'bg-gray-300'
              }`}
            >
              <span className="sr-only">Toggle availability</span>
              <span
                className={`inline-block h-6 w-6 transform rounded-full bg-white transition-transform ${
                  product.isAvailable ? 'translate-x-7' : 'translate-x-1'
                }`}
              />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
