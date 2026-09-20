import { useState } from 'react';
import api from '../api/axios';
import { AuthContext } from './authContext';

/**
 * Reads a persisted session so a page refresh keeps the user logged in. Done lazily as
 * useState's initializer rather than in an effect, so the first render already knows the user
 * and protected routes never flash a loading state.
 */
function readStoredUser() {
  const storedUser = localStorage.getItem('user');
  const token = localStorage.getItem('token');
  if (!storedUser || !token) return null;

  try {
    return JSON.parse(storedUser);
  } catch {
    // Corrupt payload: drop the unusable session instead of crashing on boot.
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    return null;
  }
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(readStoredUser);

  const persistSession = (authResponse) => {
    const userData = { email: authResponse.email, role: authResponse.role };
    localStorage.setItem('token', authResponse.token);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
  };

  const login = async (email, password) => {
    const { data } = await api.post('/auth/login', { email, password });
    persistSession(data);
  };

  const signup = async (name, email, password, role) => {
    const { data } = await api.post('/auth/signup', { name, email, password, role });
    persistSession(data);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  const value = { user, login, signup, logout };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
