import React, { useEffect, useState } from 'react';
import { healthApi } from '../services/api';
import type { HealthResponse } from '../types/api';
import { Activity, Database, CheckCircle2, AlertTriangle, XCircle, RefreshCw } from 'lucide-react';

export const Header: React.FC = () => {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchHealth = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await healthApi.getHealth();
      setHealth(response.data);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to connect to backend service');
      }
      setHealth(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();
    // Poll health every 30 seconds
    const interval = setInterval(fetchHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  return (
    <header className="h-16 bg-slate-900 border-b border-slate-800 px-6 flex items-center justify-between sticky top-0 z-20">
      <div className="flex items-center gap-3">
        <h1 className="text-sm font-semibold text-slate-200">
          Environmental Monitoring & Laboratory System
        </h1>
        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-slate-800 text-slate-300 border border-slate-700">
          ISO 17025 Ready
        </span>
      </div>

      <div className="flex items-center gap-4">
        {/* Backend Connectivity Status Indicator */}
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-950/60 border border-slate-800 text-xs">
          <Activity className="w-3.5 h-3.5 text-slate-400" />
          <span className="text-slate-400">Backend:</span>
          {loading ? (
            <span className="inline-flex items-center gap-1.5 text-amber-400">
              <RefreshCw className="w-3 h-3 animate-spin" />
              Connecting...
            </span>
          ) : error ? (
            <span className="inline-flex items-center gap-1.5 text-rose-400" title={error}>
              <XCircle className="w-3.5 h-3.5" />
              Offline
            </span>
          ) : health?.status === 'UP' ? (
            <span className="inline-flex items-center gap-1.5 text-emerald-400">
              <CheckCircle2 className="w-3.5 h-3.5" />
              Online ({health.version})
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 text-amber-400">
              <AlertTriangle className="w-3.5 h-3.5" />
              Degraded
            </span>
          )}

          {/* Database indicator */}
          {health && (
            <div className="ml-2 pl-2 border-l border-slate-800 flex items-center gap-1.5">
              <Database className="w-3 h-3 text-slate-400" />
              <span className={health.database.connected ? 'text-emerald-400' : 'text-rose-400'}>
                {health.database.databaseType}
              </span>
            </div>
          )}

          <button
            onClick={fetchHealth}
            className="ml-1 p-1 text-slate-400 hover:text-slate-200 rounded transition"
            title="Refresh Status"
          >
            <RefreshCw className={`w-3 h-3 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>

        {/* User indicator placeholder */}
        <div className="flex items-center gap-2 pl-2 border-l border-slate-800">
          <div className="w-7 h-7 rounded-full bg-teal-500/10 border border-teal-500/20 text-teal-300 flex items-center justify-center text-xs font-semibold">
            AQ
          </div>
          <div className="text-xs">
            <span className="block text-slate-200 font-medium leading-tight">Scientist</span>
            <span className="text-[10px] text-slate-500">Local Session</span>
          </div>
        </div>
      </div>
    </header>
  );
};
