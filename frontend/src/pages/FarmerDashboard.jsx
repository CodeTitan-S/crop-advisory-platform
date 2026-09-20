import DashboardTabs from '../components/DashboardTabs';

const TABS = [
  { to: '/farmer', label: 'My Farms' },
  { to: '/farmer/create-farm', label: 'Add Farm' },
  { to: '/farmer/advisory-requests', label: 'Advisory Requests' },
  { to: '/farmer/disease-reports', label: 'Disease Reports' },
];

export default function FarmerDashboard() {
  return <DashboardTabs title="Farmer Dashboard" tabs={TABS} />;
}
