import { getAnalytics } from '../../api/adminService';
import useFetch from '../../hooks/useFetch';

function StatCard({ label, value, hint }) {
  return (
    <div className="bg-white p-4 rounded shadow border-l-4 border-green-500">
      <p className="text-sm text-gray-600">{label}</p>
      <p className="text-2xl font-bold">{value}</p>
      {hint && <p className="text-xs text-gray-500 mt-1">{hint}</p>}
    </div>
  );
}

/** Horizontal bar breakdown of a `{ key: count }` map. */
function Breakdown({ title, counts }) {
  const entries = Object.entries(counts ?? {});
  const max = Math.max(1, ...entries.map(([, count]) => count));

  return (
    <div className="bg-white p-4 rounded shadow">
      <h3 className="font-semibold mb-3">{title}</h3>
      {entries.length === 0 ? (
        <p className="text-gray-500 text-sm">No data yet.</p>
      ) : (
        <ul className="space-y-2">
          {entries.map(([key, count]) => (
            <li key={key}>
              <div className="flex justify-between text-sm mb-1">
                <span>{key.replace(/_/g, ' ')}</span>
                <span className="font-medium">{count}</span>
              </div>
              <div className="h-2 bg-gray-100 rounded">
                <div
                  className="h-2 bg-green-500 rounded"
                  style={{ width: `${(count / max) * 100}%` }}
                />
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default function AdminAnalytics() {
  const { data, loading, error } = useFetch(getAnalytics, [], 'Failed to load analytics');

  if (loading) return <p className="text-gray-600">Loading analytics...</p>;
  if (error) return <p className="text-red-600">{error}</p>;
  if (!data) return <p className="text-gray-500">No analytics available.</p>;

  const weekly = data.requestsPerWeek ?? [];
  const maxWeekly = Math.max(1, ...weekly.map((week) => week.count));

  return (
    <div>
      <h2 className="text-xl font-semibold mb-1">Platform Analytics</h2>
      <p className="text-sm text-gray-600 mb-4">Platform-wide activity at a glance.</p>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <StatCard label="Users" value={data.totalUsers} />
        <StatCard label="Farms" value={data.totalFarms} />
        <StatCard label="Advisory requests" value={data.totalAdvisoryRequests} />
        <StatCard label="Disease reports" value={data.totalDiseaseReports} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Breakdown title="Users by role" counts={data.usersByRole} />
        <Breakdown title="Requests by status" counts={data.requestsByStatus} />
        <Breakdown title="Reports by status" counts={data.reportsByStatus} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white p-4 rounded shadow">
          <h3 className="font-semibold mb-1">Advisory requests per week</h3>
          <p className="text-xs text-gray-500 mb-3">Last 8 ISO weeks</p>
          <ul className="space-y-2">
            {weekly.map((week) => (
              <li key={week.weekStarting}>
                <div className="flex justify-between text-sm mb-1">
                  <span>Week of {week.weekStarting}</span>
                  <span className="font-medium">{week.count}</span>
                </div>
                <div className="h-2 bg-gray-100 rounded">
                  <div
                    className="h-2 bg-blue-500 rounded"
                    style={{ width: `${(week.count / maxWeekly) * 100}%` }}
                  />
                </div>
              </li>
            ))}
          </ul>
        </div>

        <div className="bg-white p-4 rounded shadow">
          <h3 className="font-semibold mb-1">Most reported issues</h3>
          <p className="text-xs text-gray-500 mb-3">
            Knowledge-base entries ranked by mentions in disease report descriptions
          </p>
          {(data.topDiseases ?? []).length === 0 ? (
            <p className="text-gray-500 text-sm">
              No matches yet. Add knowledge base entries whose names appear in farmer reports.
            </p>
          ) : (
            <ol className="list-decimal list-inside space-y-1">
              {data.topDiseases.map((disease) => (
                <li key={disease.name} className="flex justify-between">
                  <span>{disease.name}</span>
                  <span className="font-medium">{disease.count}</span>
                </li>
              ))}
            </ol>
          )}
        </div>
      </div>
    </div>
  );
}
