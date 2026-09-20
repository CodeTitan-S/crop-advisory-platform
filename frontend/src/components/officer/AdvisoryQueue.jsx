import { useState } from 'react';
import { getOfficerQueue, assignRequest, respondToRequest, closeRequest } from '../../api/advisoryService';
import useFetch from '../../hooks/useFetch';
import getErrorMessage from '../../utils/errorMessage';

export default function AdvisoryQueue() {
  const { data, loading, error, refetch } = useFetch(getOfficerQueue, [], 'Failed to load queue');
  const [responseText, setResponseText] = useState({}); // id -> text

  const handleAssign = async (id) => {
    try {
      await assignRequest(id);
      refetch();
    } catch (err) {
      alert(getErrorMessage(err, 'Error'));
    }
  };

  const handleRespond = async (id) => {
    const text = responseText[id];
    if (!text) return alert('Response text required');
    try {
      await respondToRequest(id, text);
      setResponseText((prev) => ({ ...prev, [id]: '' }));
      refetch();
    } catch (err) {
      alert(getErrorMessage(err, 'Error'));
    }
  };

  const handleClose = async (id) => {
    try {
      await closeRequest(id);
      refetch();
    } catch (err) {
      alert(getErrorMessage(err, 'Error'));
    }
  };

  if (loading) return <p>Loading queue...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  const requests = data ?? [];

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Advisory Requests</h2>
      {requests.length === 0 ? (
        <p>No pending requests.</p>
      ) : (
        <div className="space-y-4">
          {requests.map((req) => (
            <div key={req.id} className="bg-white p-4 rounded shadow border-l-4 border-green-500">
              <div className="flex justify-between">
                <div>
                  <p className="font-semibold">{req.questionText}</p>
                  <p className="text-sm text-gray-600">Farm ID: {req.farmId} | Status: {req.status}</p>
                </div>
                <div className="space-x-2">
                  {req.status === 'PENDING' && (
                    <button
                      onClick={() => handleAssign(req.id)}
                      className="bg-blue-500 text-white px-3 py-1 rounded"
                    >
                      Assign to Me
                    </button>
                  )}
                  {req.status === 'ASSIGNED' && (
                    <>
                      <input
                        type="text"
                        placeholder="Response..."
                        value={responseText[req.id] || ''}
                        onChange={(e) => setResponseText((prev) => ({ ...prev, [req.id]: e.target.value }))}
                        className="border px-2 py-1 rounded"
                      />
                      <button
                        onClick={() => handleRespond(req.id)}
                        className="bg-green-500 text-white px-3 py-1 rounded"
                      >
                        Respond
                      </button>
                    </>
                  )}
                  {req.status === 'RESPONDED' && (
                    <button
                      onClick={() => handleClose(req.id)}
                      className="bg-gray-500 text-white px-3 py-1 rounded"
                    >
                      Close
                    </button>
                  )}
                </div>
              </div>
              {req.responseText && (
                <p className="mt-2 text-sm text-gray-700"><strong>Response:</strong> {req.responseText}</p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
