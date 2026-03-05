import { useState } from 'react';
import { listSecrets } from '../api/secretsApi';

export default function ListSecrets() {
  const [path, setPath] = useState('/');
  const [loading, setLoading] = useState(false);
  const [secrets, setSecrets] = useState(null);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setSecrets(null);
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

  return (
    <div className="form-section">
      <h2>List Secrets</h2>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Path Prefix</label>
          <input
            type="text"
            placeholder="/"
            value={path}
            onChange={(e) => setPath(e.target.value)}
          />
        </div>

        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? 'Loading...' : 'List Secrets'}
        </button>
      </form>

      {error && <div className="result-box error">{error}</div>}

      {secrets !== null && (
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
