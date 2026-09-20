import api, { unwrap } from './axios';

export const getEntries = () => unwrap(api.get('/knowledge-base'));

export const getEntry = (id) => unwrap(api.get(`/knowledge-base/${id}`));

export const createEntry = (entryData) => unwrap(api.post('/knowledge-base', entryData));

export const updateEntry = (id, entryData) =>
  unwrap(api.put(`/knowledge-base/${id}`, entryData));

export const deleteEntry = (id) => unwrap(api.delete(`/knowledge-base/${id}`));
