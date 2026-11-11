import React, { useEffect, useState } from 'react';
import { siteApi } from '../services/api';
import type { MonitoringSiteDto, WaterSourceCategory } from '../types/api';
import { MapPin, Plus, Search, Filter, AlertCircle, CheckCircle2, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const LocationsPage: React.FC = () => {
  const [sites, setSites] = useState<MonitoringSiteDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [search, setSearch] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const { hasRole } = useAuth();

  // New site form state
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [category, setCategory] = useState<WaterSourceCategory>('RIVER');
  const [description, setDescription] = useState('');
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');

  const fetchSites = () => {
    setLoading(true);
    siteApi.getAll()
      .then(res => setSites(res.data))
      .catch(err => setError(err.response?.data?.message || 'Failed to fetch monitoring sites'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchSites();
  }, []);

  const handleCreateSite = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await siteApi.create({
        code,
        name,
        waterSourceCategory: category,
        description,
        latitude: latitude ? parseFloat(latitude) : undefined,
        longitude: longitude ? parseFloat(longitude) : undefined,
        active: true
      });
      setIsModalOpen(false);
      setCode('');
      setName('');
      setDescription('');
      fetchSites();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create site');
    }
  };

  const filteredSites = sites.filter(s => 
    s.name.toLowerCase().includes(search.toLowerCase()) || 
    s.code.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <MapPin className="w-5 h-5 text-teal-400" />
            Monitoring Sites Directory
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Registered collection stations, GPS coordinates, and water body classifications.
          </p>
        </div>

        {hasRole(['ADMINISTRATOR', 'ENVIRONMENTAL_FIELD_OFFICER']) && (
          <button 
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-2 px-3 py-2 rounded-lg bg-teal-600 hover:bg-teal-500 text-white text-xs font-medium border border-teal-500/30 transition-colors"
          >
            <Plus className="w-4 h-4" />
            Register Site
          </button>
        )}
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-3 text-rose-300 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Search Bar */}
      <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between gap-4">
        <div className="flex items-center gap-2 flex-1 max-w-md bg-slate-950 px-3 py-2 rounded-lg border border-slate-800 text-xs text-slate-400">
          <Search className="w-4 h-4 text-slate-500" />
          <input 
            type="text" 
            placeholder="Search by site code or name..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="bg-transparent border-none outline-none w-full text-white placeholder-slate-500"
          />
        </div>
        <div className="text-xs text-slate-400">
          Total Sites: <span className="font-semibold text-white">{filteredSites.length}</span>
        </div>
      </div>

      {/* Sites Grid */}
      {loading ? (
        <div className="p-12 text-center text-slate-400 text-sm">Loading monitoring stations...</div>
      ) : filteredSites.length === 0 ? (
        <div className="p-12 text-center rounded-xl bg-slate-900 border border-slate-800">
          <MapPin className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-200">No Monitoring Sites Found</h3>
          <p className="text-xs text-slate-400 max-w-md mx-auto mt-1">
            No matching stations in registry. Register your first water monitoring point above.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredSites.map(site => (
            <div key={site.id} className="p-5 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-colors">
              <div className="flex items-center justify-between mb-2">
                <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-teal-500/10 text-teal-400 border border-teal-500/20">
                  {site.code}
                </span>
                <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-300">
                  {site.waterSourceCategory}
                </span>
              </div>
              <h3 className="text-base font-semibold text-white">{site.name}</h3>
              <p className="text-xs text-slate-400 mt-1 line-clamp-2">{site.description || 'No description recorded.'}</p>
              
              <div className="mt-4 pt-4 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-500">
                <span>GPS: {site.latitude ? `${site.latitude}, ${site.longitude}` : 'N/A'}</span>
                <span className="flex items-center gap-1 text-emerald-400">
                  <CheckCircle2 className="w-3.5 h-3.5" /> Active
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Site Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white">Register Monitoring Site</h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateSite} className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Site Code</label>
                <input 
                  type="text" required value={code} onChange={e => setCode(e.target.value)}
                  placeholder="e.g. SITE-HUD-05"
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Station Name</label>
                <input 
                  type="text" required value={name} onChange={e => setName(e.target.value)}
                  placeholder="e.g. Hudson River Sampling Dock"
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Water Source Category</label>
                <select 
                  value={category} onChange={e => setCategory(e.target.value as WaterSourceCategory)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                >
                  <option value="RIVER">RIVER</option>
                  <option value="LAKE">LAKE</option>
                  <option value="ESTUARY">ESTUARY</option>
                  <option value="COASTAL">COASTAL</option>
                  <option value="GROUNDWATER">GROUNDWATER</option>
                  <option value="RESERVOIR">RESERVOIR</option>
                  <option value="TAP_WATER">TAP_WATER</option>
                  <option value="WASTEWATER">WASTEWATER</option>
                </select>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Latitude</label>
                  <input 
                    type="number" step="0.000001" value={latitude} onChange={e => setLatitude(e.target.value)}
                    placeholder="40.7128"
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Longitude</label>
                  <input 
                    type="number" step="0.000001" value={longitude} onChange={e => setLongitude(e.target.value)}
                    placeholder="-74.0060"
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Description</label>
                <textarea 
                  rows={2} value={description} onChange={e => setDescription(e.target.value)}
                  placeholder="Environmental condition notes..."
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
                  Save Station
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
