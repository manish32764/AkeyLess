import { useState } from 'react';
import { listSecrets, getAllSecretValues } from '../api/secretsApi';

export default function ListSecrets() {
  const [path, setPath] = useState('/');
  const [loading, setLoading] = useState(false);
  const [loadingValues, setLoadingValues] = useState(false);
  const [secrets, setSecrets] = useState(null);
  const [values, setValues] = useState(null);
  const [error, setError] = useState(null);

  const handleList = async (e) => {
    e.preventDefault();
    setLoading(true);
    setSecrets(null);
    setValues(null);
    setError(null);
    try {
      const data = await listSecrets(path);
      setSecrets(data.secrets);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Unexpected error');
    } finally {
      setLoading(false);
    }
  };

  const handleRetrieveAll = async () => {
    setLoadingValues(true);
    setValues(null);
    setError(null);
    try {
      const data = await getAllSecretValues(path);
      setValues(data.values);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Unexpected error');
    } finally {
      setLoadingValues(false);
    }
  };

  return (
    <div className="form-section">
      <h2>List &amp; Retrieve Secrets</h2>
      <form onSubmit={handleList}>
        <div className="form-group">
          <label>Path Prefix</label>
          <input
            type="text"
            placeholder="/"
            value={path}
            onChange={(e) => setPath(e.target.value)}
          />
        </div>

        <div className="form-actions">
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Loading...' : 'List Secrets'}
          </button>
          <button
            type="button"
            className="btn-secondary"
            disabled={loadingValues}
            onClick={handleRetrieveAll}
          >
            {loadingValues ? 'Fetching...' : 'Retrieve All Values'}
          </button>
        </div>
      </form>

      {error && <div className="result-box error">{error}</div>}

      {values !== null && (
        Object.keys(values).length === 0
          ? <p className="empty-state">No secrets found under this path.</p>
          : (
            <div className="values-table-wrap">
              <table className="values-table">
                <thead>
                  <tr>
                    <th>Secret Path</th>
                    <th>Value</th>
                  </tr>
                </thead>
                <tbody>
                  {Object.entries(values).map(([name, val]) => (
                    <tr key={name}>
                      <td className="secret-name">{name}</td>
                      <td className="secret-val">{val}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
      )}

      {secrets !== null && values === null && (
        secrets.length === 0
          ? <p className="empty-state">No static secrets found under this path.</p>
          : (
            <ul className="secret-list">
              {secrets.map((s) => (
                <li key={s} className="secret-item">{s}</li>
              ))}
            </ul>
          )
      )}
    </div>
  );
}
