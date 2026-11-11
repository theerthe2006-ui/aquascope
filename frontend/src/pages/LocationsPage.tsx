import React from 'react';
import { MapPin, Plus } from 'lucide-react';

export const LocationsPage: React.FC = () => {
  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <MapPin className="w-5 h-5 text-teal-400" />
            Monitoring Sites
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Registered collection stations, GPS coordinates, and water body classifications.
          </p>
        </div>
        <button 
          disabled 
          className="flex items-center gap-2 px-3 py-2 rounded-lg bg-teal-600/50 text-slate-300 text-xs font-medium cursor-not-allowed border border-teal-500/30"
          title="Available in Phase 3"
        >
          <Plus className="w-4 h-4" />
          Add Site (Phase 3)
        </button>
      </div>

      <div className="p-12 text-center rounded-xl bg-slate-900 border border-slate-800">
        <MapPin className="w-12 h-12 text-slate-600 mx-auto mb-3" />
        <h3 className="text-base font-semibold text-slate-200">Monitoring Sites Directory</h3>
        <p className="text-xs text-slate-400 max-w-md mx-auto mt-1">
          Flyway V1 schema migration created the persistent <code className="text-teal-300">monitoring_sites</code> table. Interactive GIS and station management will activate in Phase 3.
        </p>
      </div>
    </div>
  );
};
