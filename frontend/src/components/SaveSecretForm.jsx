import { useState } from 'react';
import { saveSecret } from '../api/secretsApi';

export default function SaveSecretForm() {
  const [name, setName] = useState('');
  const [value, setValue] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setResult(null);
    try {
      const data = await saveSecret(name, value, description);
      setResult({ type: 'success', message: data.message });
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Unexpected error';
      setResult({ type: 'error', message: msg });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="form-section">
      <h2>Save Key-Value Secret</h2>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Secret Path (Key)</label>
          <input
            type="text"
            placeholder="/my-app/db-password"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label>Secret Value</label>
          <textarea
            placeholder="Enter the secret value..."
            value={value}
            onChange={(e) => setValue(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label>Description (optional)</label>
          <input
            type="text"
            placeholder="What is this secret for?"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? 'Saving...' : 'Save Secret'}
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
