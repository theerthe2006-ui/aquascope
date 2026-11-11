import React, { useEffect, useState } from 'react';
import { sampleApi, siteApi } from '../services/api';
import type { WaterSampleDto, MonitoringSiteDto, DataOrigin } from '../types/api';
import { FlaskConical, Search, Plus, AlertCircle, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const SamplesPage: React.FC = () => {
  const [samples, setSamples] = useState<WaterSampleDto[]>([]);
  const [sites, setSites] = useState<MonitoringSiteDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [search, setSearch] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const { hasRole } = useAuth();

  // Form state
  const [sampleIdentifier, setSampleIdentifier] = useState('');
  const [monitoringSiteId, setMonitoringSiteId] = useState<number | ''>('');
  const [sampleVolume, setSampleVolume] = useState('1.0');
  const [collectionSource, setCollectionSource] = useState('SURFACE');
  const [dataOrigin, setDataOrigin] = useState<DataOrigin>('MANUAL');
  const [phLevel, setPhLevel] = useState('7.2');
  const [temperature, setTemperature] = useState('18.5');
  const [notes, setNotes] = useState('');

  const loadData = () => {
    setLoading(true);
    Promise.all([sampleApi.getAll(), siteApi.getAll()])
      .then(([sampleRes, siteRes]) => {
        setSamples(sampleRes.data);
        setSites(siteRes.data);
        if (siteRes.data.length > 0) {
          setMonitoringSiteId(siteRes.data[0].id || '');
        }
      })
      .catch(err => setError(err.response?.data?.message || 'Failed to load samples'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateSample = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!monitoringSiteId) {
      alert('Please select a monitoring site');
      return;
    }
    try {
      await sampleApi.create({
        sampleIdentifier: sampleIdentifier.trim() || undefined,
        monitoringSiteId: Number(monitoringSiteId),
        sampleVolume: parseFloat(sampleVolume),
        volumeUnit: 'L',
        collectionSource,
        dataOrigin,
        phLevel: phLevel ? parseFloat(phLevel) : undefined,
        waterTemperatureCelsius: temperature ? parseFloat(temperature) : undefined,
        status: 'REGISTERED',
        collectionDate: new Date().toISOString(),
        notes
      });
      setIsModalOpen(false);
      setSampleIdentifier('');
      setNotes('');
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to register sample');
    }
  };

  const filteredSamples = samples.filter(s => 
    s.sampleIdentifier.toLowerCase().includes(search.toLowerCase()) ||
    (s.monitoringSiteCode && s.monitoringSiteCode.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <FlaskConical className="w-5 h-5 text-teal-400" />
            Water Samples Registry
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Tracking sample identification, field collection conditions, and chain of custody.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {hasRole(['ADMINISTRATOR', 'ENVIRONMENTAL_FIELD_OFFICER', 'LABORATORY_RESEARCHER']) && (
            <button 
              onClick={() => setIsModalOpen(true)}
              className="flex items-center gap-2 px-3 py-2 rounded-lg bg-teal-600 hover:bg-teal-500 text-white text-xs font-medium border border-teal-500/30 transition-colors"
            >
              <Plus className="w-4 h-4" />
              Register Sample
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-3 text-rose-300 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Search and Filters */}
      <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between gap-4">
        <div className="flex items-center gap-2 flex-1 max-w-md bg-slate-950 px-3 py-2 rounded-lg border border-slate-800 text-xs text-slate-400">
          <Search className="w-4 h-4 text-slate-500" />
          <input 
            type="text"
            placeholder="Search by Sample Identifier or site code..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="bg-transparent border-none outline-none w-full text-white placeholder-slate-500"
          />
        </div>
        <div className="text-xs text-slate-400">
          Total Samples: <span className="font-semibold text-white">{filteredSamples.length}</span>
        </div>
      </div>

      {/* Table view */}
      {loading ? (
        <div className="p-12 text-center text-slate-400 text-sm">Loading samples...</div>
      ) : filteredSamples.length === 0 ? (
        <div className="p-12 text-center rounded-xl bg-slate-900 border border-slate-800">
          <FlaskConical className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-200">No Samples Found</h3>
          <p className="text-xs text-slate-400 max-w-md mx-auto mt-1">
            No registered water samples match your query. Register a new field collection sample above.
          </p>
        </div>
      ) : (
        <div className="rounded-xl border border-slate-800 bg-slate-900 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950 text-slate-400 uppercase font-medium border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3">Identifier</th>
                  <th className="px-4 py-3">Station</th>
                  <th className="px-4 py-3">Collection Date</th>
                  <th className="px-4 py-3">Volume</th>
                  <th className="px-4 py-3">Water Telemetry</th>
                  <th className="px-4 py-3">Data Origin</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredSamples.map(sample => (
                  <tr key={sample.id} className="hover:bg-slate-800/30">
                    <td className="px-4 py-3 font-semibold text-teal-300">{sample.sampleIdentifier}</td>
                    <td className="px-4 py-3">{sample.monitoringSiteCode || 'N/A'}</td>
                    <td className="px-4 py-3">{sample.collectionDate ? sample.collectionDate.substring(0, 10) : 'N/A'}</td>
                    <td className="px-4 py-3">{sample.sampleVolume} {sample.volumeUnit}</td>
                    <td className="px-4 py-3">
                      pH: {sample.phLevel ?? 'N/A'} | Temp: {sample.waterTemperatureCelsius ? `${sample.waterTemperatureCelsius}°C` : 'N/A'}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                        sample.dataOrigin === 'SIMULATED' ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20' :
                        sample.dataOrigin === 'DEVICE' ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20' :
                        'bg-teal-500/10 text-teal-400 border border-teal-500/20'
                      }`}>
                        {sample.dataOrigin}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="px-2 py-0.5 rounded text-[10px] bg-slate-800 text-slate-300">
                        {sample.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Register Sample Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full p-6 space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white">Register Water Sample</h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateSample} className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Sample Identifier (Optional auto-gen)</label>
                <input 
                  type="text" value={sampleIdentifier} onChange={e => setSampleIdentifier(e.target.value)}
                  placeholder="e.g. SMP-HUDSON-001"
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Monitoring Station</label>
                <select 
                  value={monitoringSiteId} onChange={e => setMonitoringSiteId(Number(e.target.value))}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                >
                  {sites.map(site => (
                    <option key={site.id} value={site.id}>{site.code} - {site.name}</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Volume (Liters)</label>
                  <input 
                    type="number" step="0.1" required value={sampleVolume} onChange={e => setSampleVolume(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Collection Source</label>
                  <input 
                    type="text" required value={collectionSource} onChange={e => setCollectionSource(e.target.value)}
                    placeholder="e.g. SURFACE, DEPTH_1M"
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">pH Level</label>
                  <input 
                    type="number" step="0.1" value={phLevel} onChange={e => setPhLevel(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Water Temp (°C)</label>
                  <input 
                    type="number" step="0.1" value={temperature} onChange={e => setTemperature(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Scientific Data Provenance</label>
                <select 
                  value={dataOrigin} onChange={e => setDataOrigin(e.target.value as DataOrigin)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                >
                  <option value="MANUAL">MANUAL (Verified Field/Laboratory Collection)</option>
                  <option value="SIMULATED">SIMULATED (Benchmarking / Synthetic Test Data)</option>
                  <option value="DEVICE">DEVICE (In-Situ Autonomous Telemetry)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Field Observation Notes</label>
                <textarea 
                  rows={2} value={notes} onChange={e => setNotes(e.target.value)}
                  placeholder="Water clarity, weather observations, or net mesh specs..."
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button 
                  type="button" onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300 text-xs font-medium"
                >
                  Cancel
                </button>
                <button 
                  type="submit"
                  className="px-4 py-2 rounded-lg bg-teal-500 text-slate-950 text-xs font-semibold hover:bg-teal-400"
                >
                  Save Sample Record
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
