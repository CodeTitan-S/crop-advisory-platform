import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
});

// Auth endpoints: a 401 here means "bad credentials", not "session expired".
const AUTH_PATHS = ['/auth/login', '/auth/signup'];

// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: handle expired sessions globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthRequest = AUTH_PATHS.some((path) => error.config?.url?.includes(path));
    if (error.response?.status === 401 && !isAuthRequest) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

/**
 * Unwraps the backend's ApiResponse envelope so callers receive the payload directly.
 * Usage: `unwrap(api.get('/farms'))`
 * Falls back to the raw body when a response does not carry the `{ data: ... }` envelope
 * (e.g. a plain string or a different shape), so callers never get `undefined` by surprise.
 */
export const unwrap = (request) =>
  request.then((response) =>
    response.data && typeof response.data === 'object' && 'data' in response.data
      ? response.data.data
      : response.data
  );

export default api;
