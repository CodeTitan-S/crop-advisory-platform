import { useMemo, useState } from 'react';
import { getOfficerQueue, assignRequest, respondToRequest, closeRequest } from '../../api/advisoryService';
import useFetch from '../../hooks/useFetch';
import getErrorMessage from '../../utils/errorMessage';
import { applyQueueControls, formatAge } from '../../utils/queue';
import QueueToolbar from './QueueToolbar';
import StatusBadge from '../StatusBadge';

/** Workflow order of the advisory state machine, used for filtering and status sorting. */
const STATUSES = ['PENDING', 'ASSIGNED', 'RESPONDED', 'CLOSED'];

export default function AdvisoryQueue() {
  const { data, loading, error, refetch } = useFetch(getOfficerQueue, [], 'Failed to load queue');
  const [responseText, setResponseText] = useState({}); // id -> text
  const [status, setStatus] = useState('ALL');
  const [sort, setSort] = useState('age-desc');

  const all = data ?? [];
  const requests = useMemo(
    () => applyQueueControls(data ?? [], { status, sort }, STATUSES),
    [data, status, sort]
  );

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

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Advisory Requests</h2>

      <QueueToolbar
        statuses={STATUSES}
        status={status}
        onStatusChange={setStatus}
        sort={sort}
        onSortChange={setSort}
        shown={requests.length}
        total={all.length}
      />

      {all.length === 0 ? (
        <p>No pending requests.</p>
      ) : requests.length === 0 ? (
        <p className="text-gray-500">No {status} requests in the queue.</p>
      ) : (
        <div className="space-y-4">
          {requests.map((req) => (
            <div key={req.id} className="bg-white p-4 rounded shadow border-l-4 border-green-500">
              <div className="flex justify-between">
                <div>
                  <p className="font-semibold">{req.questionText}</p>
                  <p className="text-sm text-gray-600">
                    Farm ID: {req.farmId} | Age: {formatAge(req.createdAt)}
                  </p>
                  <div className="mt-1">
                    <StatusBadge status={req.status} />
                  </div>
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
