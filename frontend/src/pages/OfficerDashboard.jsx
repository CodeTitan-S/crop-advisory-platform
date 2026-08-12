import { Outlet, Link, useLocation } from 'react-router-dom';

export default function OfficerDashboard() {
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
      <h1 className="text-2xl font-bold mb-4">Officer Dashboard</h1>
      <div className="flex space-x-2 mb-0 border-b-2 border-green-700">
        <Link to="/officer" className={linkClass('/officer')}>Advisory Queue</Link>
        <Link to="/officer/disease-reports" className={linkClass('/officer/disease-reports')}>Disease Reports</Link>
      </div>
      <div className="bg-white p-6 rounded-b shadow">
        <Outlet />
      </div>
    </div>
  );
}