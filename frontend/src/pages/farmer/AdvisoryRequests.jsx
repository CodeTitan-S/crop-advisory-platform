import { useEffect, useState } from 'react';
import { getMyRequests } from '../../api/advisoryService';
import { Link } from 'react-router-dom';

export default function AdvisoryRequests() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchRequests();
  }, []);

  const fetchRequests = async () => {
    try {
      const response = await getMyRequests();
      setRequests(response.data.data);
    } catch (err) {
      setError('Failed to load requests');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <p>Loading requests...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  const statusColor = (status) => {
    switch (status) {
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'ASSIGNED': return 'bg-blue-100 text-blue-800';
      case 'RESPONDED': return 'bg-green-100 text-green-800';
      case 'CLOSED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100';
    }
  };

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
                <span className={`px-2 py-1 rounded text-xs font-medium ${statusColor(req.status)}`}>
                  {req.status}
                </span>
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