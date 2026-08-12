import { useEffect, useState } from 'react';
import { getMyReports } from '../../api/diseaseService';
import { Link } from 'react-router-dom';

export default function DiseaseReportList() {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchReports();
  }, []);

  const fetchReports = async () => {
    try {
      const response = await getMyReports();
      setReports(response.data.data);
    } catch (err) {
      setError('Failed to load disease reports');
    } finally {
      setLoading(false);
    }
  };

  const statusColor = (status) => {
    switch (status) {
      case 'REPORTED': return 'bg-yellow-100 text-yellow-800';
      case 'UNDER_REVIEW': return 'bg-blue-100 text-blue-800';
      case 'RESOLVED': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100';
    }
  };

  if (loading) return <p>Loading reports...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold">My Disease Reports</h2>
        <Link
          to="/farmer/disease-reports/new"
          className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
        >
          Report New Disease
        </Link>
      </div>
      {reports.length === 0 ? (
        <p className="text-gray-500">No reports submitted yet.</p>
      ) : (
        <div className="space-y-4">
          {reports.map((report) => (
            <div key={report.id} className="bg-white p-4 rounded shadow border-l-4 border-red-400">
              <div className="flex justify-between">
                <div>
                  <p className="font-semibold">{report.description}</p>
                  <p className="text-sm text-gray-600">
                    Reported on {new Date(report.createdAt).toLocaleDateString()}
                  </p>
                </div>
                <span className={`px-2 py-1 rounded text-xs font-medium ${statusColor(report.status)}`}>
                  {report.status}
                </span>
              </div>
              {report.imageUrl && (
                <img src={report.imageUrl} alt="disease" className="w-32 h-32 object-cover mt-2" />
              )}
              {report.resolutionNotes && (
                <p className="mt-2 text-sm text-gray-700">
                  <strong>Officer Resolution:</strong> {report.resolutionNotes}
                </p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}