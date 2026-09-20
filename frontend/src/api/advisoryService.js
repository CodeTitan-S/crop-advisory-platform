import api, { unwrap } from './axios';

export const submitAdvisoryRequest = (data) =>
  unwrap(api.post('/advisory-requests', data));

export const getMyRequests = () =>
  unwrap(api.get('/advisory-requests/my-requests'));

export const getOfficerQueue = () =>
  unwrap(api.get('/advisory-requests/queue'));

export const assignRequest = (id) =>
  unwrap(api.put(`/advisory-requests/${id}/assign`));

export const respondToRequest = (id, responseText) =>
  unwrap(api.put(`/advisory-requests/${id}/respond`, { responseText }));

export const closeRequest = (id) =>
  unwrap(api.put(`/advisory-requests/${id}/close`));
