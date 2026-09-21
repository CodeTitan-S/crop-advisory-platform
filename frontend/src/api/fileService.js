import api, { unwrap } from './axios';

/**
 * Uploads one photo and resolves to the URL to store on the report.
 * Axios sets the multipart content type (with boundary) for a FormData body.
 */
export const uploadPhoto = (file) => {
  const form = new FormData();
  form.append('file', file);
  return unwrap(api.post('/files', form));
};
