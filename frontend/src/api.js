import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT token to every request
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('driveease_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Handle 401 responses (token expired)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('driveease_token');
      localStorage.removeItem('driveease_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// ========== Auth ==========
export const login = (username, password) =>
  api.post('/auth/login', { username, password });

export const register = (data) =>
  api.post('/auth/register', data);

// ========== Vehicles ==========
export const getVehicles = () =>
  api.get('/vehicles');

export const getVehicleById = (id) =>
  api.get(`/vehicles/${id}`);

export const addVehicle = (data) =>
  api.post('/vehicles', data);

export const updateVehicle = (id, data) =>
  api.put(`/vehicles/${id}`, data);

export const deleteVehicle = (id) =>
  api.delete(`/vehicles/${id}`);

export const searchVehicles = (params) =>
  api.get('/vehicles/search', { params });

export const calculatePrice = (data) =>
  api.post('/vehicles/calculate-price', data);

// ========== Users ==========
export const getUsers = () =>
  api.get('/users');

export const deactivateUser = (id) =>
  api.delete(`/users/${id}`);

export default api;
