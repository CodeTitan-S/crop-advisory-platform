import api, { unwrap } from './axios';

export const getSeasonLogs = (farmId) =>
  unwrap(api.get(`/farms/${farmId}/season-logs`));

export const getSeasonLog = (farmId, logId) =>
  unwrap(api.get(`/farms/${farmId}/season-logs/${logId}`));

export const logSeason = (farmId, logData) =>
  unwrap(api.post(`/farms/${farmId}/season-logs`, logData));

export const updateSeasonLog = (farmId, logId, logData) =>
  unwrap(api.put(`/farms/${farmId}/season-logs/${logId}`, logData));

export const deleteSeasonLog = (farmId, logId) =>
  unwrap(api.delete(`/farms/${farmId}/season-logs/${logId}`));
