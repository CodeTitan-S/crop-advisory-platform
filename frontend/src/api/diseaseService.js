import api from './axios';

export const submitDiseaseReport = (data) =>
  api.post('/disease-reports', data);

export const getMyReports = () =>
  api.get('/disease-reports/my-reports');

export const getOfficerDiseaseQueue = () =>
  api.get('/disease-reports/queue');

export const reviewReport = (id) =>
  api.put(`/disease-reports/${id}/review`);

export const resolveReport = (id, resolutionNotes) =>
  api.put(`/disease-reports/${id}/resolve`, { resolutionNotes });