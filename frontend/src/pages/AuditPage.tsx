import React, { useEffect, useState } from 'react';
import { auditApi } from '../services/api';
import type { AuditLogDto } from '../types/api';
import { ShieldCheck, Clock, User, Tag, Terminal, RefreshCw } from 'lucide-react';

export const AuditPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLogDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAuditLogs = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await auditApi.getRecent(50);
      setLogs(res.data || []);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch audit log entries');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditLogs();
  }, []);

  return (
    <div className="space-y-6">
      {/* Title & Refresh */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-100 flex items-center gap-2.5">
            <ShieldCheck className="w-7 h-7 text-cyan-400" />
            Immutable Audit Trail & Data Provenance
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Traceable regulatory compliance record of user modifications, analysis submissions, peer reviews and data provenance.
          </p>
        </div>

        <button
          onClick={fetchAuditLogs}
          className="flex items-center gap-2 bg-slate-800 hover:bg-slate-700 text-cyan-300 border border-cyan-500/30 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all self-start sm:self-auto"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Refresh Audit Log
        </button>
      </div>

      {error && (
        <div className="p-4 rounded-xl border border-rose-500/30 bg-rose-950/20 text-rose-300 text-sm">
          {error}
        </div>
      )}

      {/* Logs Table */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/50 overflow-hidden">
        <div className="p-4 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-300">
            <Terminal className="w-4 h-4 text-cyan-400" />
            REGULATORY ACTIVITY LOG (RECENT 50 ACTIONS)
          </div>
          <span className="text-xs text-slate-500">Immutable Ledger</span>
        </div>

        {loading ? (
          <div className="py-16 text-center text-slate-500 text-sm">Loading audit entries...</div>
        ) : logs.length === 0 ? (
          <div className="py-16 text-center text-slate-400 text-sm">
            No audit records registered yet. System activities will be logged here.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 bg-slate-950/30 font-medium">
                  <th className="py-3 px-4">TIMESTAMP</th>
                  <th className="py-3 px-4">OPERATOR</th>
                  <th className="py-3 px-4">ACTION</th>
                  <th className="py-3 px-4">ENTITY TYPE</th>
                  <th className="py-3 px-4">ORIGIN</th>
                  <th className="py-3 px-4">DETAILS</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="py-3 px-4 text-slate-400 whitespace-nowrap flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-slate-500" />
                      {new Date(log.createdAt).toLocaleString()}
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <span className="inline-flex items-center gap-1.5 text-slate-200 font-semibold font-sans">
                        <User className="w-3.5 h-3.5 text-cyan-400" />
                        {log.username || 'SYSTEM'}
                      </span>
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold tracking-wide bg-cyan-950/60 text-cyan-300 border border-cyan-500/30">
                        {log.action}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-300 whitespace-nowrap flex items-center gap-1 font-sans">
                      <Tag className="w-3 h-3 text-slate-500" />
                      {log.entityType} {log.entityId ? `(#${log.entityId})` : ''}
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                        log.dataOrigin === 'SIMULATED'
                          ? 'bg-purple-950/60 text-purple-300 border border-purple-500/30'
                          : log.dataOrigin === 'DEVICE'
                          ? 'bg-blue-950/60 text-blue-300 border border-blue-500/30'
                          : 'bg-emerald-950/60 text-emerald-300 border border-emerald-500/30'
                      }`}>
                        {log.dataOrigin}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-300 font-sans max-w-md truncate">
                      {log.details || '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
