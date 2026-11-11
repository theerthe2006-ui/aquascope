import React from 'react';
import { BarChart3 } from 'lucide-react';

export const AnalyticsPage: React.FC = () => {
  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <BarChart3 className="w-5 h-5 text-teal-400" />
          Analytics & Historical Trends
        </h2>
        <p className="text-xs text-slate-400 mt-1">
          Quantitative particle metrics, shape distributions, and size comparisons.
        </p>
      </div>

      <div className="p-12 text-center rounded-xl bg-slate-900 border border-slate-800">
        <BarChart3 className="w-12 h-12 text-slate-600 mx-auto mb-3" />
        <h3 className="text-base font-semibold text-slate-200">Analytics Module Initialized</h3>
        <p className="text-xs text-slate-400 max-w-md mx-auto mt-1">
          Aggregation models adhering to verified physical units (&mu;m, particles/L) will connect directly to backend observations in Phase 5. No arbitrary or simulated metrics are displayed.
        </p>
      </div>
    </div>
  );
};
