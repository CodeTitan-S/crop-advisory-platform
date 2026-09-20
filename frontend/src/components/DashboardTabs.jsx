import { Link, Outlet, useLocation } from 'react-router-dom';

/**
 * Shared dashboard shell: a title, a row of tab links, and the nested route content.
 *
 * @param {string} title heading shown above the tabs
 * @param {Array<{to: string, label: string}>} tabs navigation entries
 */
export default function DashboardTabs({ title, tabs }) {
  const location = useLocation();

  const linkClass = (path) =>
    `px-4 py-2 rounded-t ${
      location.pathname === path
        ? 'bg-white text-green-700 font-semibold border-b-2 border-green-700'
        : 'bg-green-100 text-green-800 hover:bg-green-200'
    }`;

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">{title}</h1>
      <div className="flex space-x-2 mb-0 border-b-2 border-green-700">
        {tabs.map((tab) => (
          <Link key={tab.to} to={tab.to} className={linkClass(tab.to)}>
            {tab.label}
          </Link>
        ))}
      </div>
      <div className="bg-white p-6 rounded-b shadow">
        <Outlet />
      </div>
    </div>
  );
}
