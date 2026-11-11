import React from 'react';
import { FileText } from 'lucide-react';

export const ReportsPage: React.FC = () => {
  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <FileText className="w-5 h-5 text-teal-400" />
          Scientific Reports & Export
        </h2>
        <p className="text-xs text-slate-400 mt-1">
          Compliance-ready PDF and CSV export with full sample provenance.
        </p>
      </div>

      <div className="p-12 text-center rounded-xl bg-slate-900 border border-slate-800">
        <FileText className="w-12 h-12 text-slate-600 mx-auto mb-3" />
        <h3 className="text-base font-semibold text-slate-200">Reporting Engine Ready</h3>
        <p className="text-xs text-slate-400 max-w-md mx-auto mt-1">
          Exporting sample dossiers, analytical methods, and observation summaries with ISO 17025 traceability will be enabled in Phase 5.
        </p>
      </div>
    </div>
  );
};
