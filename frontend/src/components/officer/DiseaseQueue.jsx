import { useMemo, useState } from 'react';
import { getOfficerDiseaseQueue, reviewReport, resolveReport } from '../../api/diseaseService';
import useFetch from '../../hooks/useFetch';
import getErrorMessage from '../../utils/errorMessage';
import { applyQueueControls, formatAge } from '../../utils/queue';
import QueueToolbar from './QueueToolbar';
import StatusBadge from '../StatusBadge';

/** Workflow order of the disease report state machine, used for filtering and status sorting. */
const STATUSES = ['REPORTED', 'UNDER_REVIEW', 'RESOLVED'];

export default function DiseaseQueue() {
  const { data, loading, error, refetch } = useFetch(
    getOfficerDiseaseQueue,
    [],
    'Failed to load reports'
  );
  const [resolution, setResolution] = useState({}); // id -> text
  const [status, setStatus] = useState('ALL');
  const [sort, setSort] = useState('age-desc');

  const all = data ?? [];
  const reports = useMemo(
    () => applyQueueControls(data ?? [], { status, sort }, STATUSES),
    [data, status, sort]
  );

  const handleReview = async (id) => {
    try {
      await reviewReport(id);
      refetch();
    } catch (err) {
      alert(getErrorMessage(err, 'Error'));
    }
  };

  const handleResolve = async (id) => {
    const notes = resolution[id];
    if (!notes) return alert('Resolution notes required');
    try {
      await resolveReport(id, notes);
      setResolution((prev) => ({ ...prev, [id]: '' }));
      refetch();
    } catch (err) {
      alert(getErrorMessage(err, 'Error'));
    }
  };

  if (loading) return <p>Loading...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Disease Reports</h2>

      <QueueToolbar
        statuses={STATUSES}
        status={status}
        onStatusChange={setStatus}
        sort={sort}
        onSortChange={setSort}
        shown={reports.length}
        total={all.length}
      />

      {all.length === 0 ? (
        <p>No disease reports.</p>
      ) : reports.length === 0 ? (
        <p className="text-gray-500">No {status} reports in the queue.</p>
      ) : (
        <div className="space-y-4">
          {reports.map((r) => (
            <div key={r.id} className="bg-white p-4 rounded shadow border-l-4 border-red-400">
              <p className="font-semibold">Description: {r.description}</p>
              {r.imageUrl && <img src={r.imageUrl} alt="disease" className="w-32 h-32 object-cover my-2" />}
              <p className="text-sm text-gray-600">Age: {formatAge(r.createdAt)}</p>
              <div className="mt-1">
                <StatusBadge status={r.status} />
              </div>
              <div className="mt-2 space-x-2">
                {r.status === 'REPORTED' && (
                  <button onClick={() => handleReview(r.id)} className="bg-blue-500 text-white px-3 py-1 rounded">
                    Review
                  </button>
                )}
                {r.status === 'UNDER_REVIEW' && (
                  <>
                    <input
                      type="text"
                      placeholder="Resolution notes..."
                      value={resolution[r.id] || ''}
                      onChange={(e) => setResolution((prev) => ({ ...prev, [r.id]: e.target.value }))}
                      className="border px-2 py-1 rounded"
                    />
                    <button onClick={() => handleResolve(r.id)} className="bg-green-500 text-white px-3 py-1 rounded">
                      Resolve
                    </button>
                  </>
                )}
              </div>
              {r.resolutionNotes && (
                <p className="mt-2 text-sm text-gray-700"><strong>Resolution:</strong> {r.resolutionNotes}</p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
