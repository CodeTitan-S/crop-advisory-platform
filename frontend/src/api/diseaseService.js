import api, { unwrap } from './axios';

export const submitDiseaseReport = (data) =>
  unwrap(api.post('/disease-reports', data));

export const getMyReports = () =>
  unwrap(api.get('/disease-reports/my-reports'));

export const getOfficerDiseaseQueue = () =>
  unwrap(api.get('/disease-reports/queue'));

export const reviewReport = (id) =>
  unwrap(api.put(`/disease-reports/${id}/review`));

export const resolveReport = (id, resolutionNotes) =>
  unwrap(api.put(`/disease-reports/${id}/resolve`, { resolutionNotes }));
