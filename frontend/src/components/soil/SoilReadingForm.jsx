import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { logSoilReading } from '../../api/soilService';

export default function SoilReadingForm() {
  const { farmId } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    nitrogen: '',
    phosphorus: '',
    potassium: '',
    ph: '',
    rainfall: '',
    temperature: '',
  });
  const [error, setError] = useState('');

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const data = Object.fromEntries(
      Object.entries(form).map(([k, v]) => [k, parseFloat(v)])
    );
    try {
      await logSoilReading(farmId, data);
      navigate(`/farmer/farms/${farmId}/soil-readings`);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to log reading');
    }
  };

  const fields = [
    { label: 'Nitrogen (N)', name: 'nitrogen' },
    { label: 'Phosphorus (P)', name: 'phosphorus' },
    { label: 'Potassium (K)', name: 'potassium' },
    { label: 'pH', name: 'ph' },
    { label: 'Rainfall (mm)', name: 'rainfall' },
    { label: 'Temperature (°C)', name: 'temperature' },
  ];

  return (
    <div className="max-w-lg mx-auto bg-white p-6 rounded shadow mt-6">
      <h2 className="text-xl font-semibold mb-4">Log Soil Reading</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <form onSubmit={handleSubmit}>
        {fields.map((f) => (
          <div className="mb-4" key={f.name}>
            <label className="block text-gray-700">{f.label}</label>
            <input
              type="number"
              step="any"
              name={f.name}
              value={form[f.name]}
              onChange={handleChange}
              required
              className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
        ))}
        <button
          type="submit"
          className="w-full bg-green-600 hover:bg-green-700 text-white py-2 rounded font-semibold"
        >
          Save Reading
        </button>
      </form>
    </div>
  );
}