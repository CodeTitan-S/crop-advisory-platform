import { useAuth } from '../context/AuthContext';

export default function OfficerDashboard() {
  const { user } = useAuth();
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold">Officer Dashboard</h1>
      <p>Welcome, {user?.email}</p>
      {/* Add queue view, respond to requests later */}
    </div>
  );
}