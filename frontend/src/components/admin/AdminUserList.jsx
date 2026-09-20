import { useState } from 'react';
import { getUsers, updateUserRole, deleteUser } from '../../api/adminService';
import useFetch from '../../hooks/useFetch';
import getErrorMessage from '../../utils/errorMessage';

const ROLES = ['FARMER', 'OFFICER', 'ADMIN'];

export default function AdminUserList() {
  const { data, loading, error, refetch } = useFetch(getUsers, [], 'Failed to load users');
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');

  const runAction = async (action, successMessage) => {
    setActionError('');
    setNotice('');
    try {
      await action();
      setNotice(successMessage);
      refetch();
    } catch (err) {
      setActionError(getErrorMessage(err, 'Action failed'));
    }
  };

  const handleRoleChange = (user, role) => {
    if (role === user.role) return;
    runAction(() => updateUserRole(user.id, role), `${user.email} is now ${role}`);
  };

  const handleDelete = (user) => {
    if (!window.confirm(`Delete ${user.email}? This cannot be undone.`)) return;
    runAction(() => deleteUser(user.id), `Deleted ${user.email}`);
  };

  if (loading) return <p className="text-gray-600">Loading users...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  const users = data ?? [];

  return (
    <div>
      <h2 className="text-xl font-semibold mb-1">User Accounts</h2>
      <p className="text-sm text-gray-600 mb-4">
        {users.length} {users.length === 1 ? 'account' : 'accounts'}. Changing a role takes effect on
        the user&apos;s next login.
      </p>

      {actionError && <p className="text-red-600 mb-3">{actionError}</p>}
      {notice && <p className="text-green-700 mb-3">{notice}</p>}

      {users.length === 0 ? (
        <p className="text-gray-500">No users found.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full bg-white border">
            <thead>
              <tr className="bg-green-100">
                <th className="py-2 px-4 border text-left">Name</th>
                <th className="py-2 px-4 border text-left">Email</th>
                <th className="py-2 px-4 border text-left">Role</th>
                <th className="py-2 px-4 border text-left">Farms</th>
                <th className="py-2 px-4 border text-left">Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id} className="hover:bg-gray-50">
                  <td className="py-2 px-4 border">{user.name}</td>
                  <td className="py-2 px-4 border">{user.email}</td>
                  <td className="py-2 px-4 border">
                    <select
                      value={user.role}
                      onChange={(e) => handleRoleChange(user, e.target.value)}
                      aria-label={`Role for ${user.email}`}
                      className="border rounded px-2 py-1 focus:outline-none focus:ring-2 focus:ring-green-500"
                    >
                      {ROLES.map((role) => (
                        <option key={role} value={role}>
                          {role}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="py-2 px-4 border">{user.farmCount}</td>
                  <td className="py-2 px-4 border">
                    <button
                      onClick={() => handleDelete(user)}
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
      )}
    </div>
  );
}
