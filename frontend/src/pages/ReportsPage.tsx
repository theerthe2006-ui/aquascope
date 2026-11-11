import React from 'react';
import { FileText, Download, CheckCircle, AlertCircle } from 'lucide-react';
import api from '../services/api';

export const ReportsPage: React.FC = () => {
  const downloadReport = async (type: 'csv' | 'pdf') => {
    try {
      const response = await api.get(`/reports/samples/${type}`, {
        responseType: 'blob'
      });
      const blob = new Blob([response.data], {
        type: type === 'csv' ? 'text/csv' : 'application/pdf'
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `aquascope-samples-report.${type}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err: any) {
      alert('Failed to generate report. Ensure backend service is reachable.');
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <FileText className="w-5 h-5 text-teal-400" />
          Verifiable Reports & Scientific Export
        </h2>
        <p className="text-xs text-slate-400 mt-1">
          Export database-backed audits, sample registries, and chain-of-custody data in standardized CSV and PDF formats.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* CSV Export Card */}
        <div className="p-6 rounded-xl bg-slate-900 border border-slate-800 space-y-4">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-teal-500/10 border border-teal-500/20 rounded-lg text-teal-400">
              <FileText className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-white">Water Samples CSV Dataset</h3>
              <p className="text-xs text-slate-400">Raw tabular data suitable for statistical processing (R, Python, Excel).</p>
            </div>
          </div>
          <button 
            onClick={() => downloadReport('csv')}
            className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg bg-teal-500 hover:bg-teal-400 text-slate-950 font-semibold text-xs transition-colors"
          >
            <Download className="w-4 h-4" />
            Export CSV Dataset
          </button>
        </div>

        {/* PDF Export Card */}
        <div className="p-6 rounded-xl bg-slate-900 border border-slate-800 space-y-4">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-cyan-500/10 border border-cyan-500/20 rounded-lg text-cyan-400">
              <FileText className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-white">Formatted PDF Audit Report</h3>
              <p className="text-xs text-slate-400">Formatted documentation including station metadata and status tracking.</p>
            </div>
          </div>
          <button 
            onClick={() => downloadReport('pdf')}
            className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-semibold text-xs transition-colors"
          >
            <Download className="w-4 h-4" />
            Export Official PDF Report
          </button>
        </div>
      </div>
    </div>
  );
};
