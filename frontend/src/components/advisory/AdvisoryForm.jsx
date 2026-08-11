import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { submitAdvisoryRequest } from '../../api/advisoryService';
import { getMyFarms } from '../../api/farmService';

export default function AdvisoryForm() {
  const [farms, setFarms] = useState([]);
  const [farmId, setFarmId] = useState('');
  const [questionText, setQuestionText] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchFarms = async () => {
      try {
        const response = await getMyFarms();
        setFarms(response.data.data);
        if (response.data.data.length > 0) {
          setFarmId(response.data.data[0].id.toString());
        }
      } catch (err) {
        setError('Could not load farms');
      }
    };
    fetchFarms();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await submitAdvisoryRequest({
        farmId: parseInt(farmId),
        questionText,
      });
      navigate('/farmer/advisory-requests');
    } catch (err) {
      setError(err.response?.data?.message || 'Submission failed');
    }
  };

  return (
    <div className="max-w-lg mx-auto bg-white p-6 rounded shadow mt-6">
      <h2 className="text-xl font-semibold mb-4">Submit Advisory Request</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <label className="block text-gray-700">Select Farm</label>
          <select
            value={farmId}
            onChange={(e) => setFarmId(e.target.value)}
            required
            className="w-full px-3 py-2 border rounded"
          >
            {farms.map((farm) => (
              <option key={farm.id} value={farm.id}>
                {farm.location} ({farm.size} acres)
              </option>
            ))}
          </select>
        </div>
        <div className="mb-4">
          <label className="block text-gray-700">Your Question</label>
          <textarea
            value={questionText}
            onChange={(e) => setQuestionText(e.target.value)}
            required
            rows={4}
            className="w-full px-3 py-2 border rounded"
            placeholder="e.g., What crop should I plant this season?"
          />
        </div>
        <button
          type="submit"
          className="w-full bg-green-600 hover:bg-green-700 text-white py-2 rounded font-semibold"
        >
          Submit Request
        </button>
      </form>
    </div>
  );
}