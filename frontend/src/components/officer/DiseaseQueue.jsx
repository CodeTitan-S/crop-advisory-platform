import { useEffect, useState } from 'react';
import { getOfficerDiseaseQueue, reviewReport, resolveReport } from '../../api/diseaseService';

export default function DiseaseQueue() {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [resolution, setResolution] = useState({}); // id -> text

  const fetchQueue = async () => {
    try {
      const res = await getOfficerDiseaseQueue();
      setReports(res.data.data);
    } catch (err) {
      setError('Failed to load reports');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchQueue(); }, []);

  const handleReview = async (id) => {
    try {
      await reviewReport(id);
      fetchQueue();
    } catch (err) {
      alert(err.response?.data?.message || 'Error');
    }
  };

  const handleResolve = async (id) => {
    const notes = resolution[id];
    if (!notes) return alert('Resolution notes required');
    try {
      await resolveReport(id, notes);
      setResolution(prev => ({ ...prev, [id]: '' }));
      fetchQueue();
    } catch (err) {
      alert(err.response?.data?.message || 'Error');
    }
  };

  if (loading) return <p>Loading...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Disease Reports</h2>
      {reports.length === 0 ? (
        <p>No disease reports.</p>
      ) : (
        <div className="space-y-4">
          {reports.map((r) => (
            <div key={r.id} className="bg-white p-4 rounded shadow border-l-4 border-red-400">
              <p className="font-semibold">Description: {r.description}</p>
              {r.imageUrl && <img src={r.imageUrl} alt="disease" className="w-32 h-32 object-cover my-2" />}
              <p className="text-sm text-gray-600">Status: {r.status}</p>
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
                      onChange={(e) => setResolution(prev => ({ ...prev, [r.id]: e.target.value }))}
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