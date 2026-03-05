import { useState } from 'react';
import SaveSecretForm from './components/SaveSecretForm';
import GetSecretForm from './components/GetSecretForm';
import ListSecrets from './components/ListSecrets';
import './App.css';

const TABS = ['Save Secret', 'Get Secret', 'List Secrets'];

export default function App() {
  const [activeTab, setActiveTab] = useState(0);

  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>Akeyless POC</h1>
        <p className="subtitle">Manage key-value secrets via Akeyless API</p>
      </header>

      <nav className="tab-bar">
        {TABS.map((tab, i) => (
          <button
            key={tab}
            className={`tab-btn ${activeTab === i ? 'active' : ''}`}
            onClick={() => setActiveTab(i)}
          >
            {tab}
          </button>
        ))}
      </nav>

      <main className="tab-content">
        {activeTab === 0 && <SaveSecretForm />}
        {activeTab === 1 && <GetSecretForm />}
        {activeTab === 2 && <ListSecrets />}
      </main>
    </div>
  );
}
