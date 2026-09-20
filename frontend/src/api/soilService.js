import api, { unwrap } from './axios';

export const getSoilReadings = (farmId) =>
  unwrap(api.get(`/farms/${farmId}/soil-readings`));

export const logSoilReading = (farmId, readingData) =>
  unwrap(api.post(`/farms/${farmId}/soil-readings`, readingData));
