import { useMemo, useState } from 'react';
import {
  getOfficerQueue,
  reassignRequest,
} from '../../api/advisoryService';
import {
  getOfficerDiseaseQueue,
  reassignReport,
  adminResolveReport,
} from '../../api/diseaseService';
import { getUsers } from '../../api/adminService';
import useFetch from '../../hooks/useFetch';
import getErrorMessage from '../../utils/errorMessage';
import { applyQueueControls, formatAge } from '../../utils/queue';
import QueueToolbar from '../officer/QueueToolbar';
import StatusBadge from '../StatusBadge';

const ADVISORY_STATUSES = ['PENDING', 'ASSIGNED', 'RESPONDED', 'CLOSED'];
const DISEASE_STATUSES = ['REPORTED', 'UNDER_REVIEW', 'RESOLVED'];

/**
 * Admin oversight of both officer queues: reassign stuck work items to another officer and,
 * for disease reports, resolve them when the assigned officer is unreachable. The queues
 * themselves are read-only here — officers keep their own respond/close flows.
 */
export default function AdminOversight() {
  const advisory = useFetch(getOfficerQueue, [], 'Failed to load advisory requests');
  const disease = useFetch(getOfficerDiseaseQueue, [], 'Failed to load disease reports');
  const users = useFetch(getUsers, [], 'Failed to load officers');

  const [advisoryControls, setAdvisoryControls] = useState({ status: 'ALL', sort: 'age-desc' });
  const [diseaseControls, setDiseaseControls] = useState({ status: 'ALL', sort: 'age-desc' });
  const [selectedOfficer, setSelectedOfficer] = useState('');
  const [resolutionNotes, setResolutionNotes] = useState({}); // report id -> text
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');

  const officers = useMemo(
    () => (users.data ?? []).filter((u) => u.role === 'OFFICER'),
    [users.data]
  );

  const run = async (action, successMessage) => {
    setActionError('');
    setNotice('');
    try {
      await action();
      setNotice(successMessage);
      advisory.refetch();
      disease.refetch();
    } catch (err) {
      setActionError(getErrorMessage(err, 'Action failed'));
    }
  };

  const handleReassign = (type, id) => {
    if (!selectedOfficer) {
      setActionError('Choose an officer to reassign to first.');
      return;
    }
    const officerName = officers.find((o) => String(o.id) === selectedOfficer)?.email ?? 'officer';
    if (!window.confirm(`Reassign to ${officerName}?`)) return;
    run(
      type === 'advisory'
        ? () => reassignRequest(id, Number(selectedOfficer))
        : () => reassignReport(id, Number(selectedOfficer)),
      'Reassigned.'
    );
  };

  const handleAdminResolve = (reportId) => {
    const notes = (resolutionNotes[reportId] || '').trim();
    if (!notes) {
      setActionError('Resolution notes are required.');
      return;
    }
    run(() => adminResolveReport(reportId, notes), 'Report resolved.');
  };

  const advisoryItems = useMemo(
    () => applyQueueControls(advisory.data ?? [], advisoryControls, ADVISORY_STATUSES),
    [advisory.data, advisoryControls]
  );
  const diseaseItems = useMemo(
    () => applyQueueControls(disease.data ?? [], diseaseControls, DISEASE_STATUSES),
    [disease.data, diseaseControls]
  );

  if (advisory.loading || disease.loading || users.loading) {
    return <p className="text-gray-600">Loading queues...</p>;
  }
  if (advisory.error) return <p className="text-red-600">{advisory.error}</p>;
  if (disease.error) return <p className="text-red-600">{disease.error}</p>;

  const officerPicker = (
    <label className="flex items-center gap-2 text-sm text-gray-600 mb-4">
      Reassign to officer
      <select
        value={selectedOfficer}
        onChange={(e) => setSelectedOfficer(e.target.value)}
        aria-label="Officer to reassign to"
        className="border rounded px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-green-500"
      >
        <option value="">— choose officer —</option>
        {officers.map((o) => (
          <option key={o.id} value={o.id}>
            {o.name} ({o.email})
          </option>
        ))}
      </select>
    </label>
  );

  return (
    <div>
      <h2 className="text-xl font-semibold mb-1">Workflow Oversight</h2>
      <p className="text-sm text-gray-600 mb-4">
        Reassign stuck requests or reports to another officer, or resolve an unreachable
        officer&apos;s disease reports yourself.
      </p>

      {actionError && <p className="text-red-600 mb-3">{actionError}</p>}
      {notice && <p className="text-green-700 mb-3">{notice}</p>}
      {officers.length === 0 && (
        <p className="text-yellow-700 bg-yellow-50 border border-yellow-200 rounded p-3 mb-4">
          No officer accounts exist yet — reassignment needs at least one officer to hand work to.
        </p>
      )}

      {officerPicker}

      {/* ─── Advisory requests ─── */}
      <h3 className="font-semibold text-lg mt-6 mb-2">Advisory Requests</h3>
      <QueueToolbar
        statuses={ADVISORY_STATUSES}
        status={advisoryControls.status}
        onStatusChange={(status) => setAdvisoryControls((c) => ({ ...c, status }))}
        sort={advisoryControls.sort}
        onSortChange={(sort) => setAdvisoryControls((c) => ({ ...c, sort }))}
        shown={advisoryItems.length}
        total={(advisory.data ?? []).length}
      />
      {advisoryItems.length === 0 ? (
        <p className="text-gray-500">No advisory requests.</p>
      ) : (
        <div className="space-y-3 mb-6">
          {advisoryItems.map((req) => (
            <div key={req.id} className="bg-white p-4 rounded shadow border-l-4 border-green-500">
              <div className="flex justify-between items-start gap-4">
                <div>
                  <p className="font-semibold">{req.questionText}</p>
                  <p className="text-sm text-gray-600">
                    Farm ID: {req.farmId} | Age: {formatAge(req.createdAt)}
                  </p>
                  <div className="mt-1">
                    <StatusBadge status={req.status} />
                  </div>
                </div>
                {(req.status === 'PENDING' || req.status === 'ASSIGNED') && (
                  <button
                    onClick={() => handleReassign('advisory', req.id)}
                    className="bg-blue-500 hover:bg-blue-600 text-white px-3 py-1 rounded whitespace-nowrap"
                  >
                    Reassign
                  </button>
                )}
              </div>
              {req.responseText && (
                <p className="mt-2 text-sm text-gray-700">
                  <strong>Response:</strong> {req.responseText}
                </p>
              )}
            </div>
          ))}
        </div>
      )}

      {/* ─── Disease reports ─── */}
      <h3 className="font-semibold text-lg mt-6 mb-2">Disease Reports</h3>
      <QueueToolbar
        statuses={DISEASE_STATUSES}
        status={diseaseControls.status}
        onStatusChange={(status) => setDiseaseControls((c) => ({ ...c, status }))}
        sort={diseaseControls.sort}
        onSortChange={(sort) => setDiseaseControls((c) => ({ ...c, sort }))}
        shown={diseaseItems.length}
        total={(disease.data ?? []).length}
      />
      {diseaseItems.length === 0 ? (
        <p className="text-gray-500">No disease reports.</p>
      ) : (
        <div className="space-y-3">
          {diseaseItems.map((r) => (
            <div key={r.id} className="bg-white p-4 rounded shadow border-l-4 border-red-400">
              <div className="flex justify-between items-start gap-4">
                <div>
                  <p className="font-semibold">{r.description}</p>
                  {r.imageUrl && (
                    <img src={r.imageUrl} alt="disease" className="w-32 h-32 object-cover my-2" />
                  )}
                  <p className="text-sm text-gray-600">Age: {formatAge(r.createdAt)}</p>
                  <div className="mt-1">
                    <StatusBadge status={r.status} />
                  </div>
                </div>
                <div className="flex flex-col items-end gap-2">
                  {(r.status === 'REPORTED' || r.status === 'UNDER_REVIEW') && (
                    <button
                      onClick={() => handleReassign('disease', r.id)}
                      className="bg-blue-500 hover:bg-blue-600 text-white px-3 py-1 rounded whitespace-nowrap"
                    >
                      Reassign
                    </button>
                  )}
                  {r.status === 'UNDER_REVIEW' && (
                    <div className="flex flex-col items-end gap-1">
                      <input
                        type="text"
                        placeholder="Resolution notes..."
                        aria-label={`Resolution notes for report ${r.id}`}
                        value={resolutionNotes[r.id] || ''}
                        onChange={(e) =>
                          setResolutionNotes((prev) => ({ ...prev, [r.id]: e.target.value }))
                        }
                        className="border px-2 py-1 rounded"
                      />
                      <button
                        onClick={() => handleAdminResolve(r.id)}
                        className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded whitespace-nowrap"
                      >
                        Resolve as Admin
                      </button>
                    </div>
                  )}
                </div>
              </div>
              {r.resolutionNotes && (
                <p className="mt-2 text-sm text-gray-700">
                  <strong>Resolution:</strong> {r.resolutionNotes}
                </p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
