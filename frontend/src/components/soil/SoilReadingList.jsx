import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getSoilReadings } from '../../api/soilService';

export default function SoilReadingList() {
  const { farmId } = useParams();
  const [readings, setReadings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchReadings();
  }, [farmId]);

  const fetchReadings = async () => {
    try {
      const response = await getSoilReadings(farmId);
      setReadings(response.data.data);
    } catch (err) {
      setError('Failed to load soil readings');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <p>Loading readings...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold">Soil Readings (Farm #{farmId})</h2>
        <Link
          to={`/farmer/farms/${farmId}/soil-readings/new`}
          className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
        >
          Log New Reading
        </Link>
      </div>
      {readings.length === 0 ? (
        <p className="text-gray-500">No soil readings recorded yet.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full bg-white border">
            <thead>
              <tr className="bg-green-100">
                <th className="py-2 px-4 border">Date</th>
                <th className="py-2 px-4 border">N</th>
                <th className="py-2 px-4 border">P</th>
                <th className="py-2 px-4 border">K</th>
                <th className="py-2 px-4 border">pH</th>
                <th className="py-2 px-4 border">Rainfall (mm)</th>
                <th className="py-2 px-4 border">Temp (°C)</th>
              </tr>
            </thead>
            <tbody>
              {readings.map((r) => (
                <tr key={r.id} className="hover:bg-gray-50">
                  <td className="py-2 px-4 border">{new Date(r.recordedAt).toLocaleDateString()}</td>
                  <td className="py-2 px-4 border">{r.nitrogen}</td>
                  <td className="py-2 px-4 border">{r.phosphorus}</td>
                  <td className="py-2 px-4 border">{r.potassium}</td>
                  <td className="py-2 px-4 border">{r.ph}</td>
                  <td className="py-2 px-4 border">{r.rainfall}</td>
                  <td className="py-2 px-4 border">{r.temperature}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}