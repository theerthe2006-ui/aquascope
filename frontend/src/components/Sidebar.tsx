import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  Waves, 
  LayoutDashboard, 
  FlaskConical, 
  MapPin, 
  BarChart3, 
  FileText, 
  Cpu, 
  ShieldAlert, 
  Settings 
} from 'lucide-react';

interface NavItem {
  name: string;
  to: string;
  icon: React.ComponentType<{ className?: string }>;
}

const navItems: NavItem[] = [
  { name: 'Dashboard', to: '/', icon: LayoutDashboard },
  { name: 'Samples', to: '/samples', icon: FlaskConical },
  { name: 'Monitoring Sites', to: '/locations', icon: MapPin },
  { name: 'Analytics', to: '/analytics', icon: BarChart3 },
  { name: 'Reports', to: '/reports', icon: FileText },
  { name: 'Devices', to: '/devices', icon: Cpu },
  { name: 'Audit Logs', to: '/audit', icon: ShieldAlert },
  { name: 'Settings', to: '/settings', icon: Settings },
];

export const Sidebar: React.FC = () => {
  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col shrink-0 min-h-screen">
      {/* Brand Header */}
      <div className="h-16 flex items-center gap-3 px-6 border-b border-slate-800 bg-slate-950/40">
        <div className="w-9 h-9 rounded-lg bg-teal-500/10 border border-teal-500/20 flex items-center justify-center text-teal-400 shadow-sm shadow-teal-500/5">
          <Waves className="w-5 h-5 text-teal-400" />
        </div>
        <div>
          <span className="text-base font-bold tracking-tight text-white">AQUASCOPE</span>
          <span className="block text-[10px] uppercase font-mono tracking-wider text-teal-400/80 -mt-0.5">Microplastic Intel</span>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        <div className="px-3 pb-2 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
          Core Workflows
        </div>
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 text-sm font-medium rounded-lg transition-colors duration-150 ${
                isActive
                  ? 'bg-teal-500/10 text-teal-300 border border-teal-500/20'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`
            }
          >
            <item.icon className="w-4 h-4 shrink-0" />
            <span>{item.name}</span>
          </NavLink>
        ))}
      </nav>

      {/* Platform Status Footer in Sidebar */}
      <div className="p-4 border-t border-slate-800/80 bg-slate-950/20">
        <div className="flex items-center justify-between text-xs text-slate-400">
          <span>Release</span>
          <span className="font-mono text-cyan-400 font-semibold">Production (v1.0.0)</span>
        </div>
      </div>
    </aside>
  );
};
