import { Outlet, Link, useLocation } from 'react-router-dom';

export default function FarmerDashboard() {
  const location = useLocation();
  const isActive = (path) => location.pathname === path;

  const linkClass = (path) =>
    `px-4 py-2 rounded-t ${
      isActive(path)
        ? 'bg-white text-green-700 font-semibold border-b-2 border-green-700'
        : 'bg-green-100 text-green-800 hover:bg-green-200'
    }`;

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">Farmer Dashboard</h1>
      
      {/* Sub-navigation tabs */}
      <div className="flex space-x-2 mb-0 border-b-2 border-green-700">
        <Link to="/farmer" className={linkClass('/farmer')}>My Farms</Link>
        <Link to="/farmer/create-farm" className={linkClass('/farmer/create-farm')}>Add Farm</Link>
        <Link to="/farmer/advisory-requests" className={linkClass('/farmer/advisory-requests')}>Advisory Requests</Link>
      </div>

      {/* Render nested route components */}
      <div className="bg-white p-6 rounded-b shadow">
        <Outlet />
      </div>
    </div>
  );
}