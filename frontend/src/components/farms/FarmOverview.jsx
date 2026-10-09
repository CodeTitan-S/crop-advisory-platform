import { useParams } from 'react-router-dom';
import { getFarm } from '../../api/farmService';
import useFetch from '../../hooks/useFetch';
import getErrorMessage from '../../utils/errorMessage';

export default function FarmOverview() {
  const { farmId } = useParams();
  const { data, loading, error, refetch } = useFetch(
    () => getFarm(farmId),
    [farmId],
    'Failed to load farm details'
  );

  if (loading) return <p className="text-gray-600">Loading farm details...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  const farm = data ?? {};

  return (
    <div className="p-6">
      <h2 className="text-xl font-semibold mb-4">Farm Overview</h2>
      <div className="space-y-4">
        <div>
          <p className="font-medium">Location:</p>
          <p className="pl-2">{farm.location || '—'}</p>
        </div>
        <div>
          <p className="font-medium">Size:</p>
          <p className="pl-2">{farm.size || '—'} acres</p>
        </div>
        <div>
          <p className="font-medium">Soil Type:</p>
          <p className="pl-2">{farm.soilType || '—'}</p>
        </div>
        <div>
          <p className="font-medium">Owner:</p>
          <p className="pl-2">{farm.user?.name || '—'}</p>
        </div>
      </div>
    </div>
  );
}