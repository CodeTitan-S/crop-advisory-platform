import { useState } from 'react';
import { createFarm } from '../../api/farmService';
import { useNavigate } from 'react-router-dom';

export default function FarmCreate() {
  const [location, setLocation] = useState('');
  const [size, setSize] = useState('');
  const [soilType, setSoilType] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      await createFarm({ location, size: parseFloat(size), soilType });
      setSuccess('Farm created successfully!');
      setTimeout(() => navigate('/farmer'), 1500); // redirect to farm list after 1.5s
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create farm');
    }
  };

  return (
    <div className="max-w-lg mx-auto bg-white p-6 rounded shadow mt-6">
      <h2 className="text-xl font-semibold mb-4">Add New Farm</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      {success && <p className="text-green-600 mb-2">{success}</p>}
      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <label className="block text-gray-700">Location</label>
          <input
            type="text"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            required
            className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-green-500"
          />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700">Size (acres)</label>
          <input
            type="number"
            step="0.1"
            value={size}
            onChange={(e) => setSize(e.target.value)}
            required
            className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-green-500"
          />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700">Soil Type</label>
          <input
            type="text"
            value={soilType}
            onChange={(e) => setSoilType(e.target.value)}
            required
            className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-green-500"
            placeholder="e.g., Loamy, Clay, Sandy"
          />
        </div>
        <button
          type="submit"
          className="w-full bg-green-600 hover:bg-green-700 text-white py-2 rounded font-semibold"
        >
          Create Farm
        </button>
      </form>
    </div>
  );
}