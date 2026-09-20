import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getSeasonLog, logSeason, updateSeasonLog } from '../../api/seasonService';
import getErrorMessage from '../../utils/errorMessage';

const EMPTY_FORM = { cropPlanted: '', sowingDate: '', outcomeNotes: '' };

export default function SeasonLogForm() {
  const { farmId, logId } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(logId);

  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(isEdit);

  useEffect(() => {
    if (!isEdit) return undefined;
    let cancelled = false;

    getSeasonLog(farmId, logId)
      .then((log) => {
        if (cancelled) return;
        setForm({
          cropPlanted: log.cropPlanted ?? '',
          sowingDate: log.sowingDate ?? '',
          outcomeNotes: log.outcomeNotes ?? '',
        });
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, 'Failed to load season log'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [farmId, logId, isEdit]);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (isEdit) {
        await updateSeasonLog(farmId, logId, form);
      } else {
        await logSeason(farmId, form);
      }
      navigate(`/farmer/farms/${farmId}/season-logs`);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to save season log'));
    }
  };

  if (loading) return <p className="text-gray-600">Loading season log...</p>;

  return (
    <div className="max-w-lg mx-auto bg-white p-6 rounded shadow mt-6">
      <h2 className="text-xl font-semibold mb-4">
        {isEdit ? 'Edit Season Log' : 'Log Season'}
      </h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <label className="block text-gray-700" htmlFor="cropPlanted">
            Crop Planted
          </label>
          <input
            id="cropPlanted"
            type="text"
            name="cropPlanted"
            value={form.cropPlanted}
            onChange={handleChange}
            required
            maxLength={100}
            className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-green-500"
          />
        </div>

        <div className="mb-4">
          <label className="block text-gray-700" htmlFor="sowingDate">
            Sowing Date
          </label>
          <input
            id="sowingDate"
            type="date"
            name="sowingDate"
            value={form.sowingDate}
            onChange={handleChange}
            required
            max={new Date().toISOString().slice(0, 10)}
            className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-green-500"
          />
        </div>

        <div className="mb-4">
          <label className="block text-gray-700" htmlFor="outcomeNotes">
            Outcome Notes <span className="text-gray-400">(optional)</span>
          </label>
          <textarea
            id="outcomeNotes"
            name="outcomeNotes"
            value={form.outcomeNotes}
            onChange={handleChange}
            rows={3}
            maxLength={2000}
            placeholder="How did this season turn out?"
            className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-green-500"
          />
        </div>

        <button
          type="submit"
          className="w-full bg-green-600 hover:bg-green-700 text-white py-2 rounded font-semibold"
        >
          {isEdit ? 'Save Changes' : 'Save Season Log'}
        </button>
      </form>
    </div>
  );
}
