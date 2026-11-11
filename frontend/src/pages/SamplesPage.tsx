import React from 'react';
import { FlaskConical, Search, Filter, Plus } from 'lucide-react';

export const SamplesPage: React.FC = () => {
  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <FlaskConical className="w-5 h-5 text-teal-400" />
            Water Samples Registry
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Tracking sample identification, field collection conditions, and volume.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button 
            disabled 
            className="flex items-center gap-2 px-3 py-2 rounded-lg bg-teal-600/50 text-slate-300 text-xs font-medium cursor-not-allowed border border-teal-500/30"
            title="Available in Phase 3"
          >
            <Plus className="w-4 h-4" />
            Register Sample (Phase 3)
          </button>
        </div>
      </div>

      {/* Filter and Search Bar placeholder */}
      <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between gap-4">
        <div className="flex items-center gap-2 flex-1 max-w-md bg-slate-950 px-3 py-2 rounded-lg border border-slate-800 text-xs text-slate-400">
          <Search className="w-4 h-4 text-slate-500" />
          <span>Search by Sample ID, location code, or personnel...</span>
        </div>
        <div className="flex items-center gap-2 text-xs text-slate-400">
          <Filter className="w-4 h-4 text-slate-500" />
          <span>Filters: All Sources</span>
        </div>
      </div>

      {/* State display */}
      <div className="p-12 text-center rounded-xl bg-slate-900 border border-slate-800">
        <FlaskConical className="w-12 h-12 text-slate-600 mx-auto mb-3" />
        <h3 className="text-base font-semibold text-slate-200">Sample Management Module Initialized</h3>
        <p className="text-xs text-slate-400 max-w-md mx-auto mt-1">
          Full CRUD workflows for sample tracking, batch recording, and chain of custody will be bound in Phase 3.
        </p>
      </div>
    </div>
  );
};
