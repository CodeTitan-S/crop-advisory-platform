import { useEffect, useState } from 'react';
import { getMyFarms } from '../../api/farmService';
import { Link } from 'react-router-dom';

export default function FarmList() {
  const [farms, setFarms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchFarms();
  }, []);

  const fetchFarms = async () => {
    try {
      const response = await getMyFarms();
      setFarms(response.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load farms');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <p className="text-gray-600">Loading farms...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">My Farms</h2>
      {farms.length === 0 ? (
        <p className="text-gray-500">You haven't added any farms yet.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {farms.map((farm) => (
            <div key={farm.id} className="bg-white p-4 rounded shadow border-l-4 border-green-500">
              <h3 className="font-bold text-lg">{farm.location}</h3>
              <p><span className="font-medium">Size:</span> {farm.size} acres</p>
              <p><span className="font-medium">Soil Type:</span> {farm.soilType}</p>
              <Link
                to={`/farmer/farms/${farm.id}/soil-readings`}
                className="mt-2 inline-block text-green-700 hover:underline"
              >
                View Soil Readings →
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}