import api from './axios';

export const getMyFarms = () => api.get('/farms');

export const createFarm = (farmData) => api.post('/farms', farmData);