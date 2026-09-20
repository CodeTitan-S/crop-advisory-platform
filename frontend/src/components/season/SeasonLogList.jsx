import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getSeasonLogs, deleteSeasonLog } from '../../api/seasonService';
import useFetch from '../../hooks/useFetch';
import getErrorMessage from '../../utils/errorMessage';

/**
 * Formats a "YYYY-MM-DD" LocalDate string without going through Date's UTC parsing,
 * which would shift the day backwards in negative-offset timezones.
 */
function formatSowingDate(isoDate) {
  if (!isoDate) return '—';
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString();
}

export default function SeasonLogList() {
  const { farmId } = useParams();
  const { data, loading, error, refetch } = useFetch(
    () => getSeasonLogs(farmId),
    [farmId],
    'Failed to load season history'
  );
  const [actionError, setActionError] = useState('');

  const handleDelete = async (logId) => {
    if (!window.confirm('Delete this season log?')) return;
    setActionError('');
    try {
      await deleteSeasonLog(farmId, logId);
      refetch();
    } catch (err) {
      setActionError(getErrorMessage(err, 'Failed to delete season log'));
    }
  };

  if (loading) return <p className="text-gray-600">Loading season history...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  const logs = data ?? [];

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold">Season History (Farm #{farmId})</h2>
        <Link
          to={`/farmer/farms/${farmId}/season-logs/new`}
          className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
        >
          Log New Season
        </Link>
      </div>

      {actionError && <p className="text-red-600 mb-3">{actionError}</p>}

      {logs.length === 0 ? (
        <p className="text-gray-500">No seasons recorded for this farm yet.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full bg-white border">
            <thead>
              <tr className="bg-green-100">
                <th className="py-2 px-4 border text-left">Crop Planted</th>
                <th className="py-2 px-4 border text-left">Sowing Date</th>
                <th className="py-2 px-4 border text-left">Outcome Notes</th>
                <th className="py-2 px-4 border text-left">Actions</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id} className="hover:bg-gray-50">
                  <td className="py-2 px-4 border">{log.cropPlanted}</td>
                  <td className="py-2 px-4 border">{formatSowingDate(log.sowingDate)}</td>
                  <td className="py-2 px-4 border">{log.outcomeNotes || '—'}</td>
                  <td className="py-2 px-4 border whitespace-nowrap">
                    <Link
                      to={`/farmer/farms/${farmId}/season-logs/${log.id}/edit`}
                      className="text-green-700 hover:underline mr-3"
                    >
                      Edit
                    </Link>
                    <button
                      onClick={() => handleDelete(log.id)}
                      className="text-red-600 hover:underline"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
