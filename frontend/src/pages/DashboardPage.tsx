import React, { useEffect, useState } from 'react';
import { healthApi } from '../services/api';
import type { HealthResponse } from '../types/api';
import { 
  Activity, 
  Database, 
  Server, 
  ShieldCheck, 
  Layers, 
  AlertCircle 
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    healthApi.getHealth()
      .then((res) => {
        setHealth(res.data);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'Backend connection error');
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Top Banner */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 p-6 rounded-xl bg-gradient-to-r from-slate-900 via-slate-900 to-teal-950/40 border border-slate-800">
        <div>
          <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-teal-500/10 border border-teal-500/20 text-teal-300 text-xs font-medium mb-3">
            <span className="w-1.5 h-1.5 rounded-full bg-teal-400 animate-pulse" />
            Phase 1 Foundation Operational
          </div>
          <h2 className="text-2xl font-bold tracking-tight text-white">
            AQUASCOPE Intelligence Console
          </h2>
          <p className="text-sm text-slate-400 mt-1 max-w-2xl">
            Microplastic intelligence and water sample management platform. Full-stack verified foundation with Spring Boot 3.4.3 backend and PostgreSQL integration.
          </p>
        </div>
      </div>

      {/* Backend & Environment Status Card */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* API Backend Card */}
        <div className="p-5 rounded-xl bg-slate-900/90 border border-slate-800 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between text-slate-400 mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">REST API Service</span>
              <Server className="w-4 h-4 text-teal-400" />
            </div>
            <div className="text-xl font-bold text-white mt-1">
              {loading ? 'Checking...' : error ? 'Offline' : health?.status || 'Unknown'}
            </div>
            <p className="text-xs text-slate-400 mt-1">
              {health ? `${health.application} (${health.version})` : 'Endpoint: /api/v1/health'}
            </p>
          </div>
          <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between text-xs">
            <span className="text-slate-500">Profile</span>
            <span className="font-mono text-slate-300">{health?.environment || 'unknown'}</span>
          </div>
        </div>

        {/* Database Connectivity Card */}
        <div className="p-5 rounded-xl bg-slate-900/90 border border-slate-800 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between text-slate-400 mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Database Connection</span>
              <Database className="w-4 h-4 text-teal-400" />
            </div>
            <div className="text-xl font-bold text-white mt-1">
              {loading ? 'Probing...' : health?.database.connected ? 'Connected' : 'Unavailable'}
            </div>
            <p className="text-xs text-slate-400 mt-1">
              {health?.database.details || 'PostgreSQL 18.6 localhost:5432'}
            </p>
          </div>
          <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between text-xs">
            <span className="text-slate-500">Migrations</span>
            <span className="font-mono text-teal-300">Flyway V1 Schema</span>
          </div>
        </div>

        {/* Scientific Integrity Engine */}
        <div className="p-5 rounded-xl bg-slate-900/90 border border-slate-800 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between text-slate-400 mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider">Scientific Integrity</span>
              <ShieldCheck className="w-4 h-4 text-teal-400" />
            </div>
            <div className="text-xl font-bold text-white mt-1">
              Audited Provenance
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Mandatory Data Origin tagging: MANUAL | DEVICE | SIMULATED.
            </p>
          </div>
          <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between text-xs">
            <span className="text-slate-500">Traceability</span>
            <span className="font-mono text-emerald-400">Strict Audit Logging</span>
          </div>
        </div>
      </div>

      {/* Real-time Health Payload Inspection */}
      <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <Activity className="w-4 h-4 text-teal-400" />
            <h3 className="text-sm font-semibold text-slate-200">
              Live Backend Telemetry (/api/v1/health)
            </h3>
          </div>
          <span className="text-xs text-slate-500">Live JSON response payload</span>
        </div>

        <div className="mt-4">
          {loading ? (
            <div className="p-6 text-center text-sm text-slate-400 animate-pulse">
              Querying backend health endpoint...
            </div>
          ) : error ? (
            <div className="p-4 rounded-lg bg-rose-950/30 border border-rose-900/50 flex items-start gap-3 text-rose-300 text-sm">
              <AlertCircle className="w-5 h-5 shrink-0 text-rose-400 mt-0.5" />
              <div>
                <p className="font-medium">Backend Connection Error</p>
                <p className="text-xs text-rose-400/80 mt-1">{error}</p>
                <p className="text-xs text-slate-400 mt-2">
                  Verify the Spring Boot backend is active on <code className="bg-slate-950 px-1 py-0.5 rounded text-rose-200">http://localhost:8080</code>.
                </p>
              </div>
            </div>
          ) : (
            <pre className="p-4 rounded-lg bg-slate-950 border border-slate-800/80 font-mono text-xs text-teal-300 overflow-x-auto">
              {JSON.stringify(health, null, 2)}
            </pre>
          )}
        </div>
      </div>

      {/* Modular Product Roadmap Verification */}
      <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
        <h3 className="text-sm font-semibold text-slate-200 mb-3 flex items-center gap-2">
          <Layers className="w-4 h-4 text-teal-400" />
          AQUASCOPE Execution Roadmap Status
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
          <div className="p-3 rounded-lg bg-teal-500/5 border border-teal-500/20">
            <div className="text-xs font-semibold text-teal-300">Phase 1: Foundation</div>
            <div className="text-[11px] text-slate-400 mt-1">Backend, React Shell, CORS, Flyway V1, Health API</div>
            <span className="inline-block mt-2 text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-teal-500/20 text-teal-300">Active</span>
          </div>
          <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 opacity-60">
            <div className="text-xs font-semibold text-slate-300">Phase 2: Authentication</div>
            <div className="text-[11px] text-slate-400 mt-1">Users, Roles, Spring Security, JWT Tokens</div>
            <span className="inline-block mt-2 text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-400">Next</span>
          </div>
          <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 opacity-60">
            <div className="text-xs font-semibold text-slate-300">Phase 3: Sample Management</div>
            <div className="text-[11px] text-slate-400 mt-1">Sites, Water Samples, Geo Coordinates, Filtering</div>
            <span className="inline-block mt-2 text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-400">Planned</span>
          </div>
          <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 opacity-60">
            <div className="text-xs font-semibold text-slate-300">Phase 4: Lab & Microscopy</div>
            <div className="text-[11px] text-slate-400 mt-1">Particle Observations, Evidence Uploads, Review Workflow</div>
            <span className="inline-block mt-2 text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-400">Planned</span>
          </div>
        </div>
      </div>
    </div>
  );
};
