import api, { unwrap } from './axios';

export const getUsers = () => unwrap(api.get('/admin/users'));

export const updateUserRole = (userId, role) =>
  unwrap(api.put(`/admin/users/${userId}/role`, { role }));

export const deleteUser = (userId) => unwrap(api.delete(`/admin/users/${userId}`));

export const getAnalytics = () => unwrap(api.get('/admin/analytics'));
