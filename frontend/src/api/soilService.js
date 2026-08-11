import api from './axios';

export const getSoilReadings = (farmId) =>
  api.get(`/farms/${farmId}/soil-readings`);

export const logSoilReading = (farmId, readingData) =>
  api.post(`/farms/${farmId}/soil-readings`, readingData);