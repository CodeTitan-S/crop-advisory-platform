import { useState } from 'react';
import {
  getEntries,
  createEntry,
  updateEntry,
  deleteEntry,
} from '../../api/knowledgeBaseService';
import useFetch from '../../hooks/useFetch';
import getErrorMessage from '../../utils/errorMessage';

const EMPTY_FORM = {
  cropOrDiseaseName: '',
  description: '',
  remedyOrAdvice: '',
  source: '',
};

const FIELDS = [
  { name: 'cropOrDiseaseName', label: 'Crop or Disease Name', type: 'input' },
  { name: 'description', label: 'Description', type: 'textarea' },
  { name: 'remedyOrAdvice', label: 'Remedy or Advice', type: 'textarea' },
  { name: 'source', label: 'Source', type: 'input' },
];

export default function KnowledgeBaseManager() {
  const { data, loading, error, refetch } = useFetch(
    getEntries,
    [],
    'Failed to load knowledge base'
  );
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [formError, setFormError] = useState('');
  const [notice, setNotice] = useState('');

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const resetForm = () => {
    setForm(EMPTY_FORM);
    setEditingId(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setNotice('');
    try {
      if (editingId) {
        await updateEntry(editingId, form);
        setNotice(`Updated "${form.cropOrDiseaseName}"`);
      } else {
        await createEntry(form);
        setNotice(`Added "${form.cropOrDiseaseName}"`);
      }
      resetForm();
      refetch();
    } catch (err) {
      setFormError(getErrorMessage(err, 'Failed to save entry'));
    }
  };

  const handleEdit = (entry) => {
    setEditingId(entry.id);
    setFormError('');
    setNotice('');
    setForm({
      cropOrDiseaseName: entry.cropOrDiseaseName ?? '',
      description: entry.description ?? '',
      remedyOrAdvice: entry.remedyOrAdvice ?? '',
      source: entry.source ?? '',
    });
  };

  const handleDelete = async (entry) => {
    if (!window.confirm(`Delete "${entry.cropOrDiseaseName}"?`)) return;
    setFormError('');
    setNotice('');
    try {
      await deleteEntry(entry.id);
      if (editingId === entry.id) resetForm();
      setNotice(`Deleted "${entry.cropOrDiseaseName}"`);
      refetch();
    } catch (err) {
      setFormError(getErrorMessage(err, 'Failed to delete entry'));
    }
  };

  const entries = data ?? [];

  return (
    <div>
      <h2 className="text-xl font-semibold mb-1">Knowledge Base</h2>
      <p className="text-sm text-gray-600 mb-4">
        Reference entries that officers draw on when responding to farmers.
      </p>

      <form onSubmit={handleSubmit} className="border rounded p-4 mb-6 bg-gray-50">
        <h3 className="font-semibold mb-3">
          {editingId ? 'Edit entry' : 'Add a new entry'}
        </h3>
        {formError && <p className="text-red-600 mb-2">{formError}</p>}

        {FIELDS.map((field) => (
          <div className="mb-3" key={field.name}>
            <label className="block text-gray-700 text-sm" htmlFor={field.name}>
              {field.label}
              {field.name === 'cropOrDiseaseName' && <span className="text-red-500"> *</span>}
            </label>
            {field.type === 'textarea' ? (
              <textarea
                id={field.name}
                name={field.name}
                value={form[field.name]}
                onChange={handleChange}
                rows={2}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            ) : (
              <input
                id={field.name}
                type="text"
                name={field.name}
                value={form[field.name]}
                onChange={handleChange}
                required={field.name === 'cropOrDiseaseName'}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            )}
          </div>
        ))}

        <div className="flex gap-2">
          <button
            type="submit"
            className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded font-semibold"
          >
            {editingId ? 'Save Changes' : 'Add Entry'}
          </button>
          {editingId && (
            <button
              type="button"
              onClick={resetForm}
              className="bg-gray-200 hover:bg-gray-300 px-4 py-2 rounded"
            >
              Cancel
            </button>
          )}
        </div>
      </form>

      {notice && <p className="text-green-700 mb-3">{notice}</p>}

      {loading && <p className="text-gray-600">Loading knowledge base...</p>}
      {error && <p className="text-red-600">{error}</p>}

      {!loading && !error && (
        entries.length === 0 ? (
          <p className="text-gray-500">No entries yet. Add the first one above.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full bg-white border">
              <thead>
                <tr className="bg-green-100">
                  <th className="py-2 px-4 border text-left">Name</th>
                  <th className="py-2 px-4 border text-left">Description</th>
                  <th className="py-2 px-4 border text-left">Remedy / Advice</th>
                  <th className="py-2 px-4 border text-left">Source</th>
                  <th className="py-2 px-4 border text-left">Actions</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((entry) => (
                  <tr key={entry.id} className="hover:bg-gray-50 align-top">
                    <td className="py-2 px-4 border font-medium">{entry.cropOrDiseaseName}</td>
                    <td className="py-2 px-4 border">{entry.description || '—'}</td>
                    <td className="py-2 px-4 border">{entry.remedyOrAdvice || '—'}</td>
                    <td className="py-2 px-4 border">{entry.source || '—'}</td>
                    <td className="py-2 px-4 border whitespace-nowrap">
                      <button
                        onClick={() => handleEdit(entry)}
                        className="text-green-700 hover:underline mr-3"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(entry)}
                        className="text-red-600 hover:underline"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}
    </div>
  );
}
