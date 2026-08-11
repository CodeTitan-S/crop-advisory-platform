import { useAuth } from '../context/AuthContext';

export default function FarmerDashboard() {
  const { user } = useAuth();
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold">Farmer Dashboard</h1>
      <p>Welcome, {user?.email}</p>
      {/* Add farm management, soil readings, advisory request forms later */}
    </div>
  );
}