import { useState } from 'react';
import { saveSecret } from '../api/secretsApi';

const emptyRow = () => ({ key: '', value: '', description: '' });

export default function SaveSecretForm() {
  const [rows, setRows] = useState([emptyRow()]);
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState(null);

  const addRow = () => setRows((r) => [...r, emptyRow()]);

  const removeRow = (i) => setRows((r) => r.filter((_, idx) => idx !== i));

  const updateRow = (i, field, val) =>
    setRows((r) => r.map((row, idx) => (idx === i ? { ...row, [field]: val } : row)));

  const handleSaveAll = async (e) => {
    e.preventDefault();
    setLoading(true);
    setResults(null);
    const out = [];
    for (const row of rows) {
      try {
        const data = await saveSecret(row.key, row.value, row.description);
        out.push({ key: row.key, type: 'success', message: data.message });
      } catch (err) {
        const msg = err.response?.data?.message || err.message || 'Unexpected error';
        out.push({ key: row.key, type: 'error', message: msg });
      }
    }
    setResults(out);
    setLoading(false);
  };

  return (
    <div className="form-section">
      <h2>Save Key-Value Secrets</h2>
      <form onSubmit={handleSaveAll}>
        <div className="kv-header">
          <span>Secret Path (Key)</span>
          <span>Value</span>
          <span>Description (optional)</span>
          <span />
        </div>

        <div className="kv-rows">
          {rows.map((row, i) => (
            <div key={i} className="kv-row">
              <input
                placeholder="/my-app/db-password"
                value={row.key}
                onChange={(e) => updateRow(i, 'key', e.target.value)}
                required
              />
              <input
                placeholder="secret value"
                value={row.value}
                onChange={(e) => updateRow(i, 'value', e.target.value)}
                required
              />
              <input
                placeholder="description"
                value={row.description}
                onChange={(e) => updateRow(i, 'description', e.target.value)}
              />
              <button
                type="button"
                className="btn-remove"
                onClick={() => removeRow(i)}
                disabled={rows.length === 1}
                title="Remove row"
              >
                ✕
              </button>
            </div>
          ))}
        </div>

        <div className="form-actions">
          <button type="button" className="btn-secondary" onClick={addRow}>
            + Add Row
          </button>
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Saving...' : `Save All (${rows.length})`}
          </button>
        </div>
      </form>

      {results && (
        <div className="save-results">
          {results.map((r, i) => (
            <div key={i} className={`result-box ${r.type}`}>
              <strong>{r.key}</strong>: {r.message}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
