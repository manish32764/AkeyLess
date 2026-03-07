import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081/api';

const client = axios.create({ baseURL: BASE_URL });

/**
 * Save (create or update) a static secret.
 * @param {string} name  - Akeyless path, e.g. /my-app/db-password
 * @param {string} value - Secret value
 * @param {string} [description] - Optional description
 */
export async function saveSecret(name, value, description = '') {
  const { data } = await client.post('/secrets', { name, value, description });
  return data;
}

/**
 * Retrieve a secret value by its Akeyless path.
 * @param {string} name - Akeyless path
 */
export async function getSecret(name) {
  const { data } = await client.get('/secrets/value', { params: { name } });
  return data;
}

/**
 * List static secrets under a path prefix.
 * @param {string} [path='/'] - Path prefix
 */
export async function listSecrets(path = '/') {
  const { data } = await client.get('/secrets/list', { params: { path } });
  return data;
}

/**
 * List all secrets under a path prefix and return their name+value pairs.
 * @param {string} [path='/'] - Path prefix
 */
export async function getAllSecretValues(path = '/') {
  const { data } = await client.get('/secrets/all-values', { params: { path } });
  return data;
}
