import React from 'react';
import { 
  BrowserRouter as Router, 
  Routes, 
  Route, 
  Navigate 
} from 'react-router-dom';
import { MainLayout } from './components/MainLayout';
import { DashboardPage } from './pages/DashboardPage';
import { SamplesPage } from './pages/SamplesPage';
import { LocationsPage } from './pages/LocationsPage';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { ReportsPage } from './pages/ReportsPage';

export const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="samples" element={<SamplesPage />} />
          <Route path="locations" element={<LocationsPage />} />
          <Route path="analytics" element={<AnalyticsPage />} />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="devices" element={<DashboardPage />} />
          <Route path="audit" element={<DashboardPage />} />
          <Route path="settings" element={<DashboardPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </Router>
  );
};

export default App;
