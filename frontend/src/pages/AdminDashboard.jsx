import DashboardTabs from '../components/DashboardTabs';

const TABS = [
  { to: '/admin', label: 'Analytics' },
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/knowledge-base', label: 'Knowledge Base' },
];

export default function AdminDashboard() {
  return <DashboardTabs title="Admin Dashboard" tabs={TABS} />;
}
