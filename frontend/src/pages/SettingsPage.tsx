import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../services/api';
import type { UserDto } from '../types/api';
import { Settings, User, Shield, Building, Plus, CheckCircle2, XCircle, X } from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const { user, hasRole } = useAuth();
  const [usersList, setUsersList] = useState<UserDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // New User Form State
  const [newUsername, setNewUsername] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newFullName, setNewFullName] = useState('');
  const [newRole, setNewRole] = useState<'ADMINISTRATOR' | 'LABORATORY_RESEARCHER' | 'ENVIRONMENTAL_FIELD_OFFICER' | 'REVIEWER'>('LABORATORY_RESEARCHER');
  const [newInstitution, setNewInstitution] = useState('');

  const fetchUsers = async () => {
    if (!hasRole(['ADMINISTRATOR'])) return;
    try {
      setLoading(true);
      setError(null);
      const res = await authApi.getAllUsers();
      setUsersList(res.data || []);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch user accounts');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [user]);

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await authApi.registerUser({
        username: newUsername,
        email: newEmail,
        password: newPassword,
        fullName: newFullName,
        role: newRole,
        institution: newInstitution,
      });
      setIsModalOpen(false);
      resetForm();
      fetchUsers();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create user account');
    }
  };

  const handleToggleStatus = async (userId: number) => {
    try {
      await authApi.toggleUserStatus(userId);
      fetchUsers();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to toggle user status');
    }
  };

  const resetForm = () => {
    setNewUsername('');
    setNewEmail('');
    setNewPassword('');
    setNewFullName('');
    setNewRole('LABORATORY_RESEARCHER');
    setNewInstitution('');
  };

  return (
    <div className="space-y-6">
      {/* Title */}
      <div>
        <h1 className="text-2xl font-bold text-slate-100 flex items-center gap-2.5">
          <Settings className="w-7 h-7 text-cyan-400" />
          Settings & Role-Based Access Control
        </h1>
        <p className="text-sm text-slate-400 mt-1">
          Manage system identity, scientific laboratory credentials, and platform security policies.
        </p>
      </div>

      {/* User Profile Card */}
      <div className="p-6 rounded-2xl border border-slate-800 bg-slate-900/50">
        <h2 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-4 flex items-center gap-2">
          <User className="w-4 h-4 text-cyan-400" />
          Current Operator Profile
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="p-4 rounded-xl border border-slate-800/80 bg-slate-950/40">
            <span className="text-xs text-slate-500 font-medium block">Full Name</span>
            <span className="text-base font-bold text-slate-100 mt-1 block">{user?.fullName || 'N/A'}</span>
            <span className="text-xs text-slate-400 mt-0.5 block font-mono">@{user?.username}</span>
          </div>

          <div className="p-4 rounded-xl border border-slate-800/80 bg-slate-950/40">
            <span className="text-xs text-slate-500 font-medium block flex items-center gap-1.5">
              <Shield className="w-3.5 h-3.5 text-cyan-400" /> Assigned Scientific Role
            </span>
            <span className="text-sm font-bold text-cyan-300 mt-1 inline-block px-2.5 py-0.5 rounded bg-cyan-950/60 border border-cyan-500/30">
              {user?.role}
            </span>
          </div>

          <div className="p-4 rounded-xl border border-slate-800/80 bg-slate-950/40">
            <span className="text-xs text-slate-500 font-medium block flex items-center gap-1.5">
              <Building className="w-3.5 h-3.5 text-cyan-400" /> Affiliated Institution
            </span>
            <span className="text-sm font-semibold text-slate-200 mt-1 block">
              {user?.institution || 'AQUASCOPE Research Network'}
            </span>
          </div>
        </div>
      </div>

      {/* Admin User Management Section */}
      {hasRole(['ADMINISTRATOR']) && (
        <div className="p-6 rounded-2xl border border-slate-800 bg-slate-900/50 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h2 className="text-sm font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-2">
                <Shield className="w-4 h-4 text-cyan-400" />
                User Accounts & Access Management
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Authorized administrators can provision new operators and manage role delegations.
              </p>
            </div>

            <button
              onClick={() => setIsModalOpen(true)}
              className="flex items-center gap-2 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-semibold px-4 py-2 rounded-lg text-xs transition-all shadow-md shadow-cyan-950/30 self-start sm:self-auto"
            >
              <Plus className="w-3.5 h-3.5 stroke-[2.5]" />
              Provision New User
            </button>
          </div>

          {error && (
            <div className="p-4 rounded-xl border border-rose-500/30 bg-rose-950/20 text-rose-300 text-sm">
              {error}
            </div>
          )}

          {loading ? (
            <div className="py-12 text-center text-slate-500 text-sm">Loading user directory...</div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-800">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-800 text-slate-400 bg-slate-950/40 font-medium">
                    <th className="py-3 px-4">USERNAME / NAME</th>
                    <th className="py-3 px-4">EMAIL</th>
                    <th className="py-3 px-4">ROLE</th>
                    <th className="py-3 px-4">INSTITUTION</th>
                    <th className="py-3 px-4">STATUS</th>
                    <th className="py-3 px-4 text-right">ACTION</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {usersList.map((u) => (
                    <tr key={u.id} className="hover:bg-slate-800/20">
                      <td className="py-3 px-4">
                        <div className="font-semibold text-slate-200">{u.fullName}</div>
                        <div className="text-slate-400 font-mono text-[11px]">@{u.username}</div>
                      </td>
                      <td className="py-3 px-4 text-slate-300">{u.email}</td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-cyan-950/60 text-cyan-300 border border-cyan-500/30">
                          {u.role}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-slate-300">{u.institution || '—'}</td>
                      <td className="py-3 px-4">
                        {u.active ? (
                          <span className="inline-flex items-center gap-1 text-emerald-400 font-medium">
                            <CheckCircle2 className="w-3.5 h-3.5" /> Active
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-rose-400 font-medium">
                            <XCircle className="w-3.5 h-3.5" /> Inactive
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          onClick={() => handleToggleStatus(u.id)}
                          className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 text-[11px] transition-colors"
                        >
                          {u.active ? 'Deactivate' : 'Activate'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Modal: Provision User */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
            <div className="flex items-center justify-between p-5 border-b border-slate-800">
              <h3 className="text-lg font-semibold text-slate-100 flex items-center gap-2">
                <Shield className="w-5 h-5 text-cyan-400" />
                Provision Operator Account
              </h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-200">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateUser} className="p-5 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Username *</label>
                  <input
                    type="text"
                    required
                    value={newUsername}
                    onChange={(e) => setNewUsername(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Full Name *</label>
                  <input
                    type="text"
                    required
                    value={newFullName}
                    onChange={(e) => setNewFullName(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Email Address *</label>
                  <input
                    type="email"
                    required
                    value={newEmail}
                    onChange={(e) => setNewEmail(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Initial Password *</label>
                  <input
                    type="password"
                    required
                    placeholder="Min 8 characters"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Scientific Role *</label>
                  <select
                    value={newRole}
                    onChange={(e) => setNewRole(e.target.value as any)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  >
                    <option value="ADMINISTRATOR">ADMINISTRATOR</option>
                    <option value="LABORATORY_RESEARCHER">LABORATORY_RESEARCHER</option>
                    <option value="ENVIRONMENTAL_FIELD_OFFICER">ENVIRONMENTAL_FIELD_OFFICER</option>
                    <option value="REVIEWER">REVIEWER</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Institution</label>
                  <input
                    type="text"
                    placeholder="e.g. Aquatic Research Lab"
                    value={newInstitution}
                    onChange={(e) => setNewInstitution(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-semibold rounded-lg text-sm"
                >
                  Create User
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
