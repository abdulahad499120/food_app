import React, { useState } from 'react';
import type { Product } from '../models/types';
import { Plus, Edit2, Trash2, X } from 'lucide-react';

interface Props {
  products: Product[];
  onToggleAvailability: (productId: string, currentStatus: boolean) => void;
  onAddProduct?: (product: Omit<Product, 'id'>) => void;
  onUpdateProduct?: (product: Product) => void;
  onDeleteProduct?: (productId: string) => void;
}

export const InventoryManager: React.FC<Props> = ({ products, onToggleAvailability, onAddProduct, onUpdateProduct, onDeleteProduct }) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  
  const [formData, setFormData] = useState({
    name: '',
    price: '',
    category_name: '',
    image: '',
    isAvailable: true
  });

  const openAddModal = () => {
    setEditingProduct(null);
    setFormData({ name: '', price: '', category_name: '', image: '', isAvailable: true });
    setIsModalOpen(true);
  };

  const openEditModal = (product: Product) => {
    setEditingProduct(product);
    setFormData({
      name: product.name,
      price: product.price.toString(),
      category_name: product.category_name,
      image: product.image,
      isAvailable: product.isAvailable
    });
    setIsModalOpen(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const productData = {
      name: formData.name,
      price: parseFloat(formData.price),
      category_name: formData.category_name,
      image: formData.image,
      isAvailable: formData.isAvailable
    };

    if (editingProduct && onUpdateProduct) {
      onUpdateProduct({ ...productData, id: editingProduct.id });
    } else if (onAddProduct) {
      onAddProduct(productData);
    }
    setIsModalOpen(false);
  };

  return (
    <div className="p-6 h-full w-full bg-gray-50 flex flex-col relative">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-extrabold text-gray-900">Live Inventory Manager</h1>
        <button 
          onClick={openAddModal}
          className="flex items-center space-x-2 bg-pink-600 hover:bg-pink-700 text-white px-4 py-2 rounded-lg font-bold shadow-sm transition-colors"
        >
          <Plus size={20} />
          <span>Add Product</span>
        </button>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 overflow-y-auto pb-8">
        {products.map(product => (
          <div key={product.id} className={`p-4 rounded-xl border flex flex-col justify-between shadow-sm transition-colors ${
            product.isAvailable ? "bg-white border-gray-200" : "bg-red-50 border-red-200"
          }`}>
            <div className="flex justify-between items-start mb-2">
              <div>
                <p className="text-xs text-gray-500 font-bold uppercase tracking-wider mb-1">{product.category_name}</p>
                <h3 className={`font-bold text-lg ${product.isAvailable ? "text-gray-900" : "text-gray-400 line-through"}`}>
                  {product.name}
                </h3>
                <p className="text-pink-600 font-bold mt-1">${(product.price || 0).toFixed(2)}</p>
              </div>
              <div className="flex space-x-2">
                <button onClick={() => openEditModal(product)} className="text-gray-400 hover:text-blue-600 transition-colors">
                  <Edit2 size={18} />
                </button>
                <button 
                  onClick={() => {
                    if (window.confirm("Are you sure you want to delete this product?") && onDeleteProduct) {
                      onDeleteProduct(product.id);
                    }
                  }} 
                  className="text-gray-400 hover:text-red-600 transition-colors"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            </div>
            
            <div className="flex items-center justify-between mt-4">
              <span className="text-sm font-medium text-gray-600">Available to order</span>
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
          </div>
        ))}
      </div>

      {isModalOpen && (
        <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 relative">
            <button onClick={() => setIsModalOpen(false)} className="absolute top-4 right-4 text-gray-400 hover:text-gray-800">
              <X size={24} />
            </button>
            
            <h2 className="text-2xl font-bold text-gray-900 mb-6">
              {editingProduct ? 'Edit Product' : 'Add New Product'}
            </h2>
            
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Product Name</label>
                <input 
                  type="text" 
                  required 
                  value={formData.name} 
                  onChange={e => setFormData({...formData, name: e.target.value})}
                  className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-pink-500 focus:border-pink-500 outline-none"
                  placeholder="e.g. Double Cheese Burger"
                />
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Price ($)</label>
                  <input 
                    type="number" 
                    required 
                    step="0.01"
                    min="0"
                    value={formData.price} 
                    onChange={e => setFormData({...formData, price: e.target.value})}
                    className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-pink-500 outline-none"
                    placeholder="0.00"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                  <input 
                    type="text" 
                    required 
                    value={formData.category_name} 
                    onChange={e => setFormData({...formData, category_name: e.target.value})}
                    className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-pink-500 outline-none"
                    placeholder="e.g. Burgers"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Image URL</label>
                <input 
                  type="url" 
                  required 
                  value={formData.image} 
                  onChange={e => setFormData({...formData, image: e.target.value})}
                  className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-pink-500 outline-none"
                  placeholder="https://..."
                />
              </div>

              <div className="flex items-center mt-4">
                <input 
                  type="checkbox" 
                  id="isAvailable" 
                  checked={formData.isAvailable}
                  onChange={e => setFormData({...formData, isAvailable: e.target.checked})}
                  className="h-5 w-5 text-pink-600 rounded border-gray-300 focus:ring-pink-500"
                />
                <label htmlFor="isAvailable" className="ml-2 block text-sm text-gray-900 font-medium">
                  Available for ordering immediately
                </label>
              </div>

              <div className="mt-8 pt-4 border-t border-gray-100 flex justify-end space-x-3">
                <button 
                  type="button" 
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 text-gray-600 font-medium hover:bg-gray-100 rounded-lg transition-colors"
                >
                  Cancel
                </button>
                <button 
                  type="submit"
                  className="px-6 py-2 bg-pink-600 hover:bg-pink-700 text-white font-bold rounded-lg shadow-sm transition-colors"
                >
                  {editingProduct ? 'Save Changes' : 'Create Product'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
