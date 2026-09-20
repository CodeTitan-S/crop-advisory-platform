import DashboardTabs from '../components/DashboardTabs';

const TABS = [
  { to: '/officer', label: 'Advisory Queue' },
  { to: '/officer/disease-reports', label: 'Disease Reports' },
];

export default function OfficerDashboard() {
  return <DashboardTabs title="Officer Dashboard" tabs={TABS} />;
}
