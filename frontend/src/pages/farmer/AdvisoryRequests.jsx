import { Link } from 'react-router-dom';
import { getMyRequests } from '../../api/advisoryService';
import useFetch from '../../hooks/useFetch';
import StatusBadge from '../../components/StatusBadge';

export default function AdvisoryRequests() {
  const { data, loading, error } = useFetch(getMyRequests, [], 'Failed to load requests');

  if (loading) return <p>Loading requests...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  const requests = data ?? [];

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold">My Advisory Requests</h2>
        <Link
          to="/farmer/advisory-requests/new"
          className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
        >
          New Request
        </Link>
      </div>
      {requests.length === 0 ? (
        <p className="text-gray-500">No requests yet.</p>
      ) : (
        <div className="space-y-4">
          {requests.map((req) => (
            <div key={req.id} className="bg-white p-4 rounded shadow border-l-4 border-green-500">
              <div className="flex justify-between items-start">
                <div>
                  <p className="font-semibold">Question: {req.questionText}</p>
                  <p className="text-sm text-gray-600">
                    Asked on {new Date(req.createdAt).toLocaleDateString()}
                  </p>
                </div>
                <StatusBadge status={req.status} />
              </div>
              {req.responseText && (
                <div className="mt-2 p-2 bg-green-50 rounded">
                  <p className="text-sm font-medium">Officer Response:</p>
                  <p className="text-sm">{req.responseText}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
