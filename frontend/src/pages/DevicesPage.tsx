import React, { useEffect, useState } from 'react';
import { deviceApi, siteApi } from '../services/api';
import type { DeviceDto, SensorReadingDto, MonitoringSiteDto, DeviceStatus } from '../types/api';
import { Cpu, Plus, Radio, Clock, AlertCircle, X, RefreshCw } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const DevicesPage: React.FC = () => {
  const [devices, setDevices] = useState<DeviceDto[]>([]);
  const [sites, setSites] = useState<MonitoringSiteDto[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<DeviceDto | null>(null);
  const [readings, setReadings] = useState<SensorReadingDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [readingsLoading, setReadingsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isReadingModalOpen, setIsReadingModalOpen] = useState(false);
  const { hasRole } = useAuth();

  // New device form
  const [deviceIdentifier, setDeviceIdentifier] = useState('');
  const [name, setName] = useState('');
  const [deviceType, setDeviceType] = useState('MULTIPARAMETER_SONDE');
  const [monitoringSiteId, setMonitoringSiteId] = useState<number | ''>('');
  const [manufacturer, setManufacturer] = useState('');
  const [firmwareVersion, setFirmwareVersion] = useState('');
  const [status, setStatus] = useState<DeviceStatus>('ACTIVE');

  // New reading form
  const [metricType, setMetricType] = useState('TURBIDITY');
  const [metricValue, setMetricValue] = useState('');
  const [unit, setUnit] = useState('NTU');

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [devRes, siteRes] = await Promise.all([
        deviceApi.getAll(),
        siteApi.getAll(),
      ]);
      setDevices(devRes.data || []);
      setSites(siteRes.data || []);
      if (devRes.data && devRes.data.length > 0 && !selectedDevice) {
        handleSelectDevice(devRes.data[0]);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch hardware devices');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSelectDevice = async (device: DeviceDto) => {
    setSelectedDevice(device);
    if (!device.id) return;
    try {
      setReadingsLoading(true);
      const res = await deviceApi.getReadings(device.id);
      setReadings(res.data || []);
    } catch (err) {
      console.error('Error fetching readings', err);
    } finally {
      setReadingsLoading(false);
    }
  };

  const handleCreateDevice = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await deviceApi.create({
        deviceIdentifier,
        name,
        deviceType,
        monitoringSiteId: monitoringSiteId === '' ? undefined : Number(monitoringSiteId),
        manufacturer,
        firmwareVersion,
        status,
      });
      setIsModalOpen(false);
      resetDeviceForm();
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to register device');
    }
  };

  const handleRecordReading = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDevice?.id) return;
    try {
      await deviceApi.recordReading({
        deviceId: selectedDevice.id,
        metricType,
        metricValue: parseFloat(metricValue),
        unit,
        dataOrigin: 'DEVICE',
        readingTimestamp: new Date().toISOString(),
      });
      setIsReadingModalOpen(false);
      setMetricValue('');
      handleSelectDevice(selectedDevice);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to record sensor reading');
    }
  };

  const resetDeviceForm = () => {
    setDeviceIdentifier('');
    setName('');
    setDeviceType('MULTIPARAMETER_SONDE');
    setMonitoringSiteId('');
    setManufacturer('');
    setFirmwareVersion('');
    setStatus('ACTIVE');
  };

  return (
    <div className="space-y-6">
      {/* Page Title & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-100 flex items-center gap-2.5">
            <Cpu className="w-7 h-7 text-cyan-400" />
            Hardware Devices & Telemetry
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Registered multiparameter sondes, optical telemetry sensors, and in-situ automated samplers.
          </p>
        </div>

        {hasRole(['ADMINISTRATOR', 'ENVIRONMENTAL_FIELD_OFFICER']) && (
          <button
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-2 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-semibold px-4 py-2.5 rounded-lg shadow-lg shadow-cyan-950/40 transition-all text-sm"
          >
            <Plus className="w-4 h-4 stroke-[2.5]" />
            Register Device
          </button>
        )}
      </div>

      {/* Scientific Integrity Notice */}
      <div className="p-4 rounded-xl border border-amber-500/30 bg-amber-950/20 text-amber-200 text-xs flex items-start gap-3">
        <AlertCircle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
        <div>
          <strong className="font-semibold block text-amber-300 mb-0.5">Scientific Sensor Distinctions:</strong>
          Physicochemical sensors (turbidity, pH, temperature, conductivity) provide vital water quality context, but cannot independently enumerate or identify microplastic polymers without spectroscopic confirmation (FTIR/Raman).
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl border border-rose-500/30 bg-rose-950/20 text-rose-300 text-sm">
          {error}
        </div>
      )}

      {/* Device List & Live Telemetry Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Device List Column */}
        <div className="lg:col-span-1 space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-400 px-1 font-medium">
            <span>REGISTERED SENSORS ({devices.length})</span>
            <button onClick={fetchData} className="hover:text-cyan-400 flex items-center gap-1">
              <RefreshCw className="w-3 h-3" /> Refresh
            </button>
          </div>

          {loading ? (
            <div className="text-center py-12 text-slate-500 text-sm">Loading hardware registry...</div>
          ) : devices.length === 0 ? (
            <div className="p-6 rounded-xl border border-slate-800 bg-slate-900/40 text-center text-slate-400 text-sm">
              No hardware devices registered yet.
            </div>
          ) : (
            devices.map((device) => {
              const isSelected = selectedDevice?.id === device.id;
              return (
                <div
                  key={device.id}
                  onClick={() => handleSelectDevice(device)}
                  className={`p-4 rounded-xl border cursor-pointer transition-all ${
                    isSelected
                      ? 'border-cyan-500/60 bg-cyan-950/20 shadow-md shadow-cyan-950/30'
                      : 'border-slate-800/80 bg-slate-900/40 hover:border-slate-700 hover:bg-slate-900/70'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-semibold text-cyan-400">{device.deviceIdentifier}</span>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-semibold uppercase ${
                          device.status === 'ACTIVE'
                            ? 'bg-emerald-950/60 text-emerald-300 border border-emerald-500/30'
                            : 'bg-amber-950/60 text-amber-300 border border-amber-500/30'
                        }`}>
                          {device.status}
                        </span>
                      </div>
                      <h3 className="font-semibold text-slate-200 text-sm mt-1">{device.name}</h3>
                      <p className="text-xs text-slate-400 mt-0.5">{device.deviceType}</p>
                    </div>
                    <Radio className={`w-4 h-4 ${device.status === 'ACTIVE' ? 'text-emerald-400 animate-pulse' : 'text-slate-500'}`} />
                  </div>

                  <div className="mt-3 pt-3 border-t border-slate-800/60 flex items-center justify-between text-xs text-slate-400">
                    <span>Site: {device.monitoringSiteCode || 'Unassigned'}</span>
                    <span className="flex items-center gap-1 text-[11px]">
                      <Clock className="w-3 h-3 text-slate-500" />
                      {device.lastPing ? new Date(device.lastPing).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Never'}
                    </span>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Live Telemetry Panel */}
        <div className="lg:col-span-2">
          {selectedDevice ? (
            <div className="p-6 rounded-2xl border border-slate-800 bg-slate-900/50 space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-cyan-400 font-bold">{selectedDevice.deviceIdentifier}</span>
                    <span className="text-slate-500">|</span>
                    <span className="text-slate-200 font-semibold">{selectedDevice.name}</span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1">
                    Manufacturer: {selectedDevice.manufacturer || 'N/A'} • Firmware: {selectedDevice.firmwareVersion || 'v1.0.0'}
                  </p>
                </div>

                <button
                  onClick={() => setIsReadingModalOpen(true)}
                  className="flex items-center gap-2 bg-slate-800 hover:bg-slate-700 text-cyan-300 border border-cyan-500/30 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all self-start sm:self-auto"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Simulate/Record Telemetry
                </button>
              </div>

              {/* Sensor Readings Stream */}
              <div>
                <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">
                  Telemetry Stream (Last 50 Transmissions)
                </h4>

                {readingsLoading ? (
                  <div className="py-12 text-center text-slate-500 text-sm">Streaming readings...</div>
                ) : readings.length === 0 ? (
                  <div className="py-8 text-center text-slate-400 text-sm border border-dashed border-slate-800 rounded-xl">
                    No sensor readings received yet for this device.
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs border-collapse">
                      <thead>
                        <tr className="border-b border-slate-800 text-slate-400 font-medium">
                          <th className="py-2.5 px-3">TIMESTAMP</th>
                          <th className="py-2.5 px-3">METRIC TYPE</th>
                          <th className="py-2.5 px-3">VALUE</th>
                          <th className="py-2.5 px-3">ORIGIN</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-800/60">
                        {readings.map((r) => (
                          <tr key={r.id} className="hover:bg-slate-800/30">
                            <td className="py-2.5 px-3 font-mono text-slate-400">
                              {new Date(r.readingTimestamp).toLocaleString()}
                            </td>
                            <td className="py-2.5 px-3 font-semibold text-slate-200">
                              {r.metricType}
                            </td>
                            <td className="py-2.5 px-3 font-mono text-cyan-300 font-bold">
                              {r.metricValue} {r.unit}
                            </td>
                            <td className="py-2.5 px-3">
                              <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                                r.dataOrigin === 'DEVICE' ? 'bg-blue-950/60 text-blue-300 border border-blue-500/30' : 'bg-purple-950/60 text-purple-300 border border-purple-500/30'
                              }`}>
                                {r.dataOrigin}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="p-12 text-center border border-slate-800 rounded-2xl bg-slate-900/30 text-slate-500 text-sm">
              Select a device from the left panel to inspect real-time sensor telemetries.
            </div>
          )}
        </div>
      </div>

      {/* Modal: Register Device */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
            <div className="flex items-center justify-between p-5 border-b border-slate-800">
              <h3 className="text-lg font-semibold text-slate-100 flex items-center gap-2">
                <Cpu className="w-5 h-5 text-cyan-400" />
                Register Hardware Device
              </h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-200">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateDevice} className="p-5 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Device Identifier *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. SONDE-HUD-01"
                    value={deviceIdentifier}
                    onChange={(e) => setDeviceIdentifier(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Device Name *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. YSI Multiparameter Sonde"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Device Type</label>
                  <select
                    value={deviceType}
                    onChange={(e) => setDeviceType(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  >
                    <option value="MULTIPARAMETER_SONDE">MULTIPARAMETER_SONDE</option>
                    <option value="OPTICAL_IN_SITU_SENSOR">OPTICAL_IN_SITU_SENSOR</option>
                    <option value="AUTOSAMPLER">AUTOSAMPLER</option>
                    <option value="WEATHER_STATION">WEATHER_STATION</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Assigned Station</label>
                  <select
                    value={monitoringSiteId}
                    onChange={(e) => setMonitoringSiteId(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  >
                    <option value="">Unassigned</option>
                    {sites.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.code} - {s.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Manufacturer</label>
                  <input
                    type="text"
                    placeholder="e.g. YSI Xylem"
                    value={manufacturer}
                    onChange={(e) => setManufacturer(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Firmware Version</label>
                  <input
                    type="text"
                    placeholder="e.g. v2.4.1"
                    value={firmwareVersion}
                    onChange={(e) => setFirmwareVersion(e.target.value)}
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
                  Save Device
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Record Telemetry */}
      {isReadingModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md overflow-hidden shadow-2xl">
            <div className="flex items-center justify-between p-5 border-b border-slate-800">
              <h3 className="text-lg font-semibold text-slate-100 flex items-center gap-2">
                <Radio className="w-5 h-5 text-cyan-400" />
                Ingest Telemetry Reading
              </h3>
              <button onClick={() => setIsReadingModalOpen(false)} className="text-slate-400 hover:text-slate-200">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleRecordReading} className="p-5 space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Target Device</label>
                <input
                  type="text"
                  disabled
                  value={`${selectedDevice?.deviceIdentifier} (${selectedDevice?.name})`}
                  className="w-full bg-slate-800/60 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-400 cursor-not-allowed"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Metric Type *</label>
                <select
                  value={metricType}
                  onChange={(e) => {
                    const val = e.target.value;
                    setMetricType(val);
                    if (val === 'TURBIDITY') setUnit('NTU');
                    else if (val === 'TEMPERATURE') setUnit('°C');
                    else if (val === 'PH') setUnit('pH');
                    else if (val === 'CONDUCTIVITY') setUnit('µS/cm');
                    else if (val === 'FLOW_RATE') setUnit('m3/s');
                  }}
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                >
                  <option value="TURBIDITY">TURBIDITY</option>
                  <option value="TEMPERATURE">TEMPERATURE</option>
                  <option value="PH">PH</option>
                  <option value="CONDUCTIVITY">CONDUCTIVITY</option>
                  <option value="FLOW_RATE">FLOW_RATE</option>
                </select>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Metric Value *</label>
                  <input
                    type="number"
                    step="any"
                    required
                    placeholder="e.g. 14.5"
                    value={metricValue}
                    onChange={(e) => setMetricValue(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Unit</label>
                  <input
                    type="text"
                    required
                    value={unit}
                    onChange={(e) => setUnit(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-cyan-500"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsReadingModalOpen(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-semibold rounded-lg text-sm"
                >
                  Ingest Reading
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
