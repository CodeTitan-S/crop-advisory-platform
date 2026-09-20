import api, { unwrap } from './axios';

export const getMyFarms = () => unwrap(api.get('/farms'));

export const createFarm = (farmData) => unwrap(api.post('/farms', farmData));
