import api from './axios';

export const submitAdvisoryRequest = (data) =>
  api.post('/advisory-requests', data);

export const getMyRequests = () =>
  api.get('/advisory-requests/my-requests');

export const getOfficerQueue = () =>
  api.get('/advisory-requests/queue');

export const assignRequest = (id) =>
  api.put(`/advisory-requests/${id}/assign`);

export const respondToRequest = (id, responseText) =>
  api.put(`/advisory-requests/${id}/respond`, { responseText });

export const closeRequest = (id) =>
  api.put(`/advisory-requests/${id}/close`);