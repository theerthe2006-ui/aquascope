import React, { useEffect, useState } from 'react';
import { dashboardApi, healthApi } from '../services/api';
import type { DashboardStatsDto, HealthResponse } from '../types/api';
import { 
  Database, 
  Server, 
  ShieldCheck, 
  AlertCircle,
  FlaskConical,
  MapPin,
  CheckCircle2,
  Clock,
  PieChart
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [stats, setStats] = useState<DashboardStatsDto | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      healthApi.getHealth().catch(() => null),
      dashboardApi.getStats().catch(() => null)
    ])
    .then(([healthRes, statsRes]) => {
      if (healthRes) setHealth(healthRes.data);
      if (statsRes) setStats(statsRes.data);
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
            Live Intelligence Active
          </div>
          <h2 className="text-2xl font-bold tracking-tight text-white">
            AQUASCOPE Intelligence Console
          </h2>
          <p className="text-sm text-slate-400 mt-1 max-w-2xl">
            Real-time scientific water monitoring and microplastic particle quantification platform with authenticated data provenance.
          </p>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-3 text-rose-300 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Metrics Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-medium">Monitoring Sites</span>
            <MapPin className="w-4 h-4 text-teal-400" />
          </div>
          <div className="text-2xl font-bold text-white">
            {stats ? stats.totalMonitoringSites : (loading ? '...' : 0)}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Geo-registered stations</div>
        </div>

        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-medium">Water Samples</span>
            <FlaskConical className="w-4 h-4 text-cyan-400" />
          </div>
          <div className="text-2xl font-bold text-white">
            {stats ? stats.totalWaterSamples : (loading ? '...' : 0)}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Samples registered</div>
        </div>

        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-medium">Pending Reviews</span>
            <Clock className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-bold text-white">
            {stats ? stats.pendingReviews : (loading ? '...' : 0)}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Awaiting peer QA</div>
        </div>

        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-medium">Particle Observations</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-white">
            {stats ? stats.totalParticleObservations : (loading ? '...' : 0)}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Quantified particles</div>
        </div>
      </div>

      {/* Backend & Environment Status Card */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center gap-3">
            <Server className="w-5 h-5 text-teal-400" />
            <div>
              <div className="text-xs text-slate-400 font-medium">Spring Boot Backend</div>
              <div className="text-sm font-semibold text-white">
                {health ? `${health.status} (${health.version})` : 'Offline / Checking...'}
              </div>
            </div>
          </div>
        </div>

        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center gap-3">
            <Database className="w-5 h-5 text-teal-400" />
            <div>
              <div className="text-xs text-slate-400 font-medium">Database Persistence</div>
              <div className="text-sm font-semibold text-white">
                {health ? health.database : 'Connecting...'}
              </div>
            </div>
          </div>
        </div>

        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center gap-3">
            <ShieldCheck className="w-5 h-5 text-teal-400" />
            <div>
              <div className="text-xs text-slate-400 font-medium">Security & Role Auth</div>
              <div className="text-sm font-semibold text-white">
                JWT Stateless / BCrypt
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Data Origin & Scientific Integrity Notice */}
      <div className="p-5 rounded-xl bg-slate-900/80 border border-slate-800">
        <h3 className="text-sm font-semibold text-white flex items-center gap-2 mb-2">
          <PieChart className="w-4 h-4 text-teal-400" />
          Scientific Data Provenance Distribution
        </h3>
        <p className="text-xs text-slate-400 mb-4">
          Strict separation of data provenance: SIMULATED (controlled test cases), MANUAL (laboratory/field officer input), and DEVICE (in-situ telemetry).
        </p>
        <div className="grid grid-cols-3 gap-4 text-center">
          <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
            <div className="text-xs text-slate-400">MANUAL</div>
            <div className="text-lg font-bold text-teal-400">{stats?.dataOriginDistribution?.MANUAL || 0}</div>
          </div>
          <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
            <div className="text-xs text-slate-400">SIMULATED</div>
            <div className="text-lg font-bold text-amber-400">{stats?.dataOriginDistribution?.SIMULATED || 0}</div>
          </div>
          <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
            <div className="text-xs text-slate-400">DEVICE</div>
            <div className="text-lg font-bold text-cyan-400">{stats?.dataOriginDistribution?.DEVICE || 0}</div>
          </div>
        </div>
      </div>
    </div>
  );
};
