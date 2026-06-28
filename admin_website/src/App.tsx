import { useState, useEffect } from 'react';
import { KanbanBoard } from './components/KanbanBoard';
import { InventoryManager } from './components/InventoryManager';
import { useOrders } from './hooks/useOrders';
import { useProducts } from './hooks/useProducts';
import { LayoutDashboard, ToggleRight, LogOut, ShieldAlert } from 'lucide-react';
import { Toaster } from 'react-hot-toast';
import { onAuthStateChanged, signInWithPopup, signOut } from 'firebase/auth';
import type { User } from 'firebase/auth';
import { auth, googleProvider } from './firebase';

function App() {
  const [activeTab, setActiveTab] = useState<'KANBAN' | 'INVENTORY'>('KANBAN');
  const [user, setUser] = useState<User | null>(null);
  const [authLoading, setAuthLoading] = useState(true);

  useEffect(() => {
    return onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setAuthLoading(false);
    });
  }, []);

  const handleLogin = async () => {
    try {
      await signInWithPopup(auth, googleProvider);
    } catch (e) {
      console.error(e);
    }
  };

  const handleLogout = () => signOut(auth);

  if (authLoading) {
    return <div className="flex h-screen w-screen items-center justify-center bg-gray-900 text-white font-bold">Checking authentication...</div>;
  }

  if (!user) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-gray-900 text-white flex-col gap-6 font-sans">
        <h1 className="text-4xl font-black tracking-tight text-white">
          ICE LAND <span className="text-pink-500">DISPATCH</span>
        </h1>
        <button 
          onClick={handleLogin}
          className="bg-pink-600 hover:bg-pink-700 text-white px-8 py-4 rounded-xl font-bold transition-all shadow-lg"
        >
          Sign in with Google
        </button>
      </div>
    );
  }

  const isAuthorized = user.uid === "W7ZWIbS7y1bFH1dNwOdUZ15NAz33" || user.email?.toLowerCase() === "abdulahad49120@gmail.com";

  if (!isAuthorized) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-gray-900 text-white flex-col gap-6 font-sans p-6 text-center">
        <ShieldAlert size={64} className="text-red-500" />
        <h1 className="text-2xl font-black">Unauthorized Access</h1>
        <p className="text-gray-400 max-w-md">Your account ({user.email}) does not have admin privileges for the Ice Land Dispatch dashboard.</p>
        <button 
          onClick={handleLogout}
          className="bg-gray-800 hover:bg-gray-700 text-white px-6 py-3 rounded-xl font-bold transition-all"
        >
          Sign Out
        </button>
      </div>
    );
  }

  return <AuthenticatedApp activeTab={activeTab} setActiveTab={setActiveTab} handleLogout={handleLogout} user={user} />;
}

// Separating the authenticated app logic so hooks don't run if unauthenticated
function AuthenticatedApp({ activeTab, setActiveTab, handleLogout, user }: { 
  activeTab: 'KANBAN' | 'INVENTORY', 
  setActiveTab: (tab: 'KANBAN' | 'INVENTORY') => void,
  handleLogout: () => void,
  user: User
}) {
  const { orders, loading: ordersLoading, updateOrderStatus } = useOrders();
  const { products, loading: productsLoading, updateProductAvailability, addProduct, updateProduct, deleteProduct } = useProducts();

  return (
    <div className="flex h-screen w-screen bg-gray-100 overflow-hidden font-sans">
      <Toaster position="top-right" />
      {/* Sidebar */}
      <div className="w-64 bg-gray-900 text-white flex flex-col">
        <div className="p-6">
          <h1 className="text-2xl font-black tracking-tight text-white">
            ICE LAND <span className="text-pink-500">DISPATCH</span>
          </h1>
          <p className="text-gray-400 text-sm mt-1">Branch: Main Street</p>
        </div>

        <nav className="flex-1 px-4 space-y-2 mt-4">
          <button
            onClick={() => setActiveTab('KANBAN')}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-bold transition-colors ${
              activeTab === 'KANBAN' ? 'bg-pink-600 text-white' : 'text-gray-400 hover:bg-gray-800 hover:text-white'
            }`}
          >
            <LayoutDashboard size={20} />
            Live Kanban
          </button>
          
          <button
            onClick={() => setActiveTab('INVENTORY')}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-bold transition-colors ${
              activeTab === 'INVENTORY' ? 'bg-pink-600 text-white' : 'text-gray-400 hover:bg-gray-800 hover:text-white'
            }`}
          >
            <ToggleRight size={20} />
            Kill-Switch
          </button>
        </nav>

        <div className="p-4 border-t border-gray-800">
           <p className="text-xs text-gray-500 mb-2 truncate px-2">{user.email}</p>
           <button
             onClick={handleLogout}
             className="w-full flex items-center gap-3 px-4 py-3 rounded-xl font-bold text-red-400 hover:bg-red-500/10 transition-colors"
           >
             <LogOut size={20} />
             Sign Out
           </button>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 overflow-hidden relative">
        {activeTab === 'KANBAN' ? (
          ordersLoading ? (
            <div className="flex h-full w-full items-center justify-center font-bold text-gray-500">Connecting to Dispatch...</div>
          ) : (
            <KanbanBoard orders={orders} onUpdateStatus={updateOrderStatus} />
          )
        ) : (
          productsLoading ? (
            <div className="flex h-full w-full items-center justify-center font-bold text-gray-500">Connecting to Inventory...</div>
          ) : (
            <InventoryManager 
              products={products} 
              onToggleAvailability={updateProductAvailability} 
              onAddProduct={addProduct}
              onUpdateProduct={updateProduct}
              onDeleteProduct={deleteProduct}
            />
          )
        )}
      </div>
    </div>
  );
}

export default App;
