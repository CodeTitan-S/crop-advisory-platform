import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { submitDiseaseReport } from '../../api/diseaseService';
import { uploadPhoto } from '../../api/fileService';
import { getMyFarms } from '../../api/farmService';
import getErrorMessage from '../../utils/errorMessage';

export default function DiseaseReportForm() {
  const [farms, setFarms] = useState([]);
  const [farmId, setFarmId] = useState('');
  const [description, setDescription] = useState('');
  const [imageUrl, setImageUrl] = useState(''); // optional, set by the upload
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchFarms = async () => {
      try {
        const myFarms = await getMyFarms();
        setFarms(myFarms);
        if (myFarms.length > 0) {
          setFarmId(myFarms[0].id.toString());
        }
      } catch (err) {
        setError(getErrorMessage(err, 'Could not load farms'));
      }
    };
    fetchFarms();
  }, []);

  // Uploads as soon as a file is chosen, so the farmer sees the photo and any rejection before
  // submitting the report itself.
  const handlePhotoChange = async (event) => {
    const input = event.target;
    const file = input.files?.[0];
    if (!file) return;

    setError('');
    setUploading(true);
    try {
      setImageUrl(await uploadPhoto(file));
    } catch (err) {
      setImageUrl('');
      setError(getErrorMessage(err, 'Could not upload the photo'));
    } finally {
      setUploading(false);
      input.value = ''; // let the same file be chosen again after a failure
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await submitDiseaseReport({
        farmId: parseInt(farmId),
        description,
        imageUrl: imageUrl || null, // optional
      });
      navigate('/farmer/disease-reports'); // go to list after submit
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to submit report'));
    }
  };

  return (
    <div className="max-w-lg mx-auto bg-white p-6 rounded shadow mt-6">
      <h2 className="text-xl font-semibold mb-4">Report Crop Disease</h2>
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
          <label className="block text-gray-700">Description of Problem</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
            rows={4}
            className="w-full px-3 py-2 border rounded"
            placeholder="Describe what you see on your crops..."
          />
        </div>

        <div className="mb-4">
          <label className="block text-gray-700" htmlFor="photo">
            Photo (optional)
          </label>
          <input
            id="photo"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            onChange={handlePhotoChange}
            disabled={uploading}
            className="w-full px-3 py-2 border rounded"
          />
          <p className="text-sm text-gray-500 mt-1">JPEG, PNG, WebP or GIF, up to 2 MB.</p>

          {uploading && <p className="text-sm text-gray-600 mt-2">Uploading photo...</p>}

          {imageUrl && (
            <div className="mt-2">
              <img
                src={imageUrl}
                alt="Selected crop problem"
                className="w-32 h-32 object-cover rounded border"
              />
              <button
                type="button"
                onClick={() => setImageUrl('')}
                className="block text-sm text-red-600 hover:underline mt-1"
              >
                Remove photo
              </button>
            </div>
          )}
        </div>

        <button
          type="submit"
          disabled={uploading}
          className="w-full bg-green-600 hover:bg-green-700 text-white py-2 rounded font-semibold disabled:opacity-60"
        >
          {uploading ? 'Uploading photo...' : 'Submit Report'}
        </button>
      </form>
    </div>
  );
}
