import { useMemo, useState } from 'react';
import { getOfficerQueue, assignRequest, respondToRequest, closeRequest, suggestCrop } from '../../api/advisoryService';
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

  const handleSuggest = async (id) => {
    try {
      await suggestCrop(id);
      refetch();
    } catch (err) {
      alert(getErrorMessage(err, 'Error'));
    }
  };

  const parseSuggestion = (jsonStr) => {
    try {
      return JSON.parse(jsonStr);
    } catch {
      return null;
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
                      {req.aiSuggestion ? (
                        <button
                          onClick={() => {
                            const suggestions = parseSuggestion(req.aiSuggestion);
                            const text = suggestions
                              ?.map((s) => `${s.crop} (${(s.confidence * 100).toFixed(0)}%)`)
                              .join(', ');
                            setResponseText((prev) => ({ ...prev, [req.id]: text || 'AI Sug: ' + req.aiSuggestion }));
                          }}
                          className="bg-purple-500 text-white px-3 py-1 rounded text-sm"
                        >
                          Use Suggestion
                        </button>
                      ) : (
                        <button
                          onClick={() => handleSuggest(req.id)}
                          className="bg-purple-600 text-white px-3 py-1 rounded text-sm"
                        >
                          Suggest Crop
                        </button>
                      )}
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
              {req.aiSuggestion && (
                <div className="mt-2 text-sm bg-purple-50 p-2 rounded border border-purple-200">
                  <p className="font-semibold text-purple-800">AI Crop Suggestions:</p>
                  <ul className="flex flex-wrap gap-2 mt-1">
                    {parseSuggestion(req.aiSuggestion)?.map((s, i) => (
                      <li key={i} className="bg-white px-2 py-1 rounded shadow-sm border border-purple-100">
                        {s.crop} <span className="text-gray-500">{(s.confidence * 100).toFixed(0)}%</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
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
