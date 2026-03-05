import { useState } from 'react';
import { getSecret } from '../api/secretsApi';

export default function GetSecretForm() {
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setResult(null);
    try {
      const data = await getSecret(name);
      setResult({
        type: 'success',
        message: `Name:  ${data.name}\nValue: ${data.value}`,
      });
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Unexpected error';
      setResult({ type: 'error', message: msg });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="form-section">
      <h2>Retrieve a Secret</h2>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Secret Path</label>
          <input
            type="text"
            placeholder="/my-app/db-password"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? 'Fetching...' : 'Get Secret'}
        </button>
      </form>

      {result && (
        <div className={`result-box ${result.type}`}>
          {result.message}
        </div>
      )}
    </div>
  );
}
