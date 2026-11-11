import React, { useEffect, useState } from 'react';
import { analysisApi, reviewApi } from '../services/api';
import type { SampleAnalysisDto } from '../types/api';
import { Microscope, AlertCircle, Eye } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const AnalyticsPage: React.FC = () => {
  const [analyses, setAnalyses] = useState<SampleAnalysisDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedAnalysis, setSelectedAnalysis] = useState<SampleAnalysisDto | null>(null);
  const [reviewComment, setReviewComment] = useState('');
  const { hasRole } = useAuth();

  const loadAnalyses = () => {
    setLoading(true);
    analysisApi.getAll()
      .then(res => setAnalyses(res.data))
      .catch(err => setError(err.response?.data?.message || 'Failed to load analyses'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadAnalyses();
  }, []);

  const handleReview = async (decision: 'APPROVED' | 'REJECTED' | 'CORRECTION_REQUESTED') => {
    if (!selectedAnalysis?.id) return;
    if (!reviewComment.trim()) {
      alert('Review comments are mandatory for scientific audits.');
      return;
    }
    try {
      await reviewApi.submitReview(selectedAnalysis.id, {
        decision,
        comments: reviewComment.trim()
      });
      setSelectedAnalysis(null);
      setReviewComment('');
      loadAnalyses();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to record peer review');
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div>
        <h2 className="text-xl font-bold text-white flex items-center gap-2">
          <Microscope className="w-5 h-5 text-teal-400" />
          Laboratory Analyses & Peer Review Workflow
        </h2>
        <p className="text-xs text-slate-400 mt-1">
          Spectroscopic particle observations, polymer characterization evidence, and formal QA verification.
        </p>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-3 text-rose-300 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="p-12 text-center text-slate-400 text-sm">Loading laboratory analysis registry...</div>
      ) : analyses.length === 0 ? (
        <div className="p-12 text-center rounded-xl bg-slate-900 border border-slate-800">
          <Microscope className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-200">No Laboratory Analyses Recorded</h3>
          <p className="text-xs text-slate-400 max-w-md mx-auto mt-1">
            Registered samples can be selected by Laboratory Researchers to perform spectroscopy and particle observation sessions.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {analyses.map(analysis => (
            <div key={analysis.id} className="p-5 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-teal-300">
                  Sample: {analysis.sampleIdentifier || `#${analysis.waterSampleId}`}
                </span>
                <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                  analysis.status === 'APPROVED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' :
                  analysis.status === 'REJECTED' ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20' :
                  analysis.status === 'SUBMITTED_FOR_REVIEW' ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20' :
                  'bg-slate-800 text-slate-300'
                }`}>
                  {analysis.status}
                </span>
              </div>

              <div className="text-xs text-slate-300 space-y-1">
                <div><span className="text-slate-500">Method:</span> {analysis.analysisMethod}</div>
                <div><span className="text-slate-500">Analyst:</span> {analysis.analystUsername || 'Unassigned'}</div>
                <div><span className="text-slate-500">Particles Quantified:</span> <strong className="text-white">{analysis.totalParticlesCounted}</strong></div>
                <div><span className="text-slate-500">Concentration:</span> {analysis.concentrationParticlesPerLiter ?? 0} particles/L</div>
              </div>

              {/* Particle observations summary */}
              {analysis.observations && analysis.observations.length > 0 && (
                <div className="p-3 bg-slate-950 rounded-lg border border-slate-800/80 text-[11px] text-slate-400">
                  <div className="font-semibold text-slate-300 mb-1">Observations ({analysis.observations.length}):</div>
                  {analysis.observations.slice(0, 3).map((obs, idx) => (
                    <div key={idx} className="flex justify-between">
                      <span>{obs.particleCount}x {obs.shape} ({obs.polymerType})</span>
                      <span>{obs.sizeMicrometers} µm</span>
                    </div>
                  ))}
                </div>
              )}

              {/* Action buttons */}
              <div className="pt-2 flex justify-end gap-2 border-t border-slate-800/80">
                {analysis.status === 'SUBMITTED_FOR_REVIEW' && hasRole(['ADMINISTRATOR', 'REVIEWER']) && (
                  <button 
                    onClick={() => setSelectedAnalysis(analysis)}
                    className="px-3 py-1.5 rounded-lg bg-teal-500 hover:bg-teal-400 text-slate-950 font-semibold text-xs flex items-center gap-1.5"
                  >
                    <Eye className="w-3.5 h-3.5" />
                    Peer Review
                  </button>
                )}
                {analysis.status === 'DRAFT' && hasRole(['ADMINISTRATOR', 'LABORATORY_RESEARCHER']) && (
                  <button 
                    onClick={async () => {
                      if (!analysis.id) return;
                      await analysisApi.submitForReview(analysis.id);
                      loadAnalyses();
                    }}
                    className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium"
                  >
                    Submit for Review
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Review Modal */}
      {selectedAnalysis && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-lg font-bold text-white">Scientific Peer Review</h3>
            <p className="text-xs text-slate-400">
              Examining spectroscopic measurements and evidence for sample <strong>{selectedAnalysis.sampleIdentifier}</strong>.
            </p>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Audit Comments & Rationale</label>
              <textarea 
                rows={3}
                required
                value={reviewComment}
                onChange={e => setReviewComment(e.target.value)}
                placeholder="State evaluation of morphological consistency, FTIR match threshold, or QA concerns..."
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-teal-500"
              />
            </div>

            <div className="flex justify-between gap-2 pt-2">
              <button 
                type="button" 
                onClick={() => setSelectedAnalysis(null)}
                className="px-3 py-2 rounded-lg bg-slate-800 text-slate-400 text-xs"
              >
                Cancel
              </button>
              <div className="flex gap-2">
                <button 
                  onClick={() => handleReview('REJECTED')}
                  className="px-3 py-2 rounded-lg bg-rose-600/20 text-rose-300 border border-rose-500/30 text-xs font-semibold hover:bg-rose-600/30"
                >
                  Reject
                </button>
                <button 
                  onClick={() => handleReview('APPROVED')}
                  className="px-4 py-2 rounded-lg bg-teal-500 text-slate-950 text-xs font-semibold hover:bg-teal-400"
                >
                  Approve Record
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
