import { useState, useEffect, useCallback } from 'react';
import {
  getVehicles,
  addVehicle,
  updateVehicle,
  deleteVehicle,
  searchVehicles,
} from '../api';
import {
  FiPlus, FiEdit2, FiTrash2, FiSearch, FiX,
  FiCalendar, FiDollarSign, FiHash, FiTruck,
  FiCheckCircle, FiAlertCircle, FiImage,
} from 'react-icons/fi';

const VEHICLE_TYPES = [
  'SUV', 'SEDAN', 'HATCHBACK', 'ESTATE',
  'CROSSOVER', 'COUPE', 'CONVERTIBLE',
  'PICKUP', 'MINIVAN', 'OTHER',
];

const emptyForm = {
  make: '',
  model: '',
  year: new Date().getFullYear(),
  type: 'SUV',
  baseDailyRate: '',
  quantityAvailable: 1,
  imageUrl: '',
  contractExpiryDate: '',
};

export default function VehiclesPage() {
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toasts, setToasts] = useState([]);

  // Filters
  const [filterName, setFilterName] = useState('');
  const [filterType, setFilterType] = useState('');
  const [filterQty, setFilterQty] = useState('');
  const [filterPickup, setFilterPickup] = useState('');
  const [filterDays, setFilterDays] = useState('');

  // Modal
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({ ...emptyForm });
  const [formErrors, setFormErrors] = useState({});
  const [saving, setSaving] = useState(false);

  // Delete confirm
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // Toast helper
  const showToast = (message, type = 'success') => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 4000);
  };

  // Fetch vehicles
  const fetchVehicles = useCallback(async () => {
    setLoading(true);
    try {
      const hasFilters = filterName || filterType || filterQty || filterPickup || filterDays;
      if (hasFilters) {
        const params = {};
        if (filterName) params.name = filterName;
        if (filterType) params.type = filterType;
        if (filterQty) params.quantity = parseInt(filterQty);
        if (filterPickup) params.pickupDate = filterPickup;
        if (filterDays) params.rentalDays = parseInt(filterDays);
        params.size = 100;
        const res = await searchVehicles(params);
        setVehicles(res.data.content || []);
      } else {
        const res = await getVehicles();
        setVehicles(res.data);
      }
    } catch (err) {
      console.error('Failed to fetch vehicles:', err);
      showToast('Failed to load vehicles', 'error');
    } finally {
      setLoading(false);
    }
  }, [filterName, filterType, filterQty, filterPickup, filterDays]);

  useEffect(() => {
    fetchVehicles();
  }, [fetchVehicles]);

  // Group vehicles by type
  const groupedVehicles = vehicles.reduce((acc, v) => {
    const type = v.type || 'OTHER';
    if (!acc[type]) acc[type] = [];
    acc[type].push(v);
    return acc;
  }, {});

  // Sort type names
  const sortedTypes = Object.keys(groupedVehicles).sort((a, b) => {
    const order = VEHICLE_TYPES;
    return order.indexOf(a) - order.indexOf(b);
  });

  // Open modal
  const openAdd = () => {
    setEditingId(null);
    setForm({ ...emptyForm });
    setFormErrors({});
    setModalOpen(true);
  };

  const openEdit = (vehicle) => {
    setEditingId(vehicle.id);
    setForm({
      make: vehicle.make || '',
      model: vehicle.model || '',
      year: vehicle.year || new Date().getFullYear(),
      type: vehicle.type || 'SUV',
      baseDailyRate: vehicle.baseDailyRate ?? '',
      quantityAvailable: vehicle.quantityAvailable ?? 1,
      imageUrl: vehicle.imageUrl || '',
      contractExpiryDate: vehicle.contractExpiryDate || '',
    });
    setFormErrors({});
    setModalOpen(true);
  };

  // Validate form
  const validate = () => {
    const errors = {};
    if (!form.make.trim()) errors.make = 'Make is required';
    if (!form.model.trim()) errors.model = 'Model is required';
    if (!form.year || form.year < 1900) errors.year = 'Valid year required';
    if (!form.baseDailyRate || parseFloat(form.baseDailyRate) <= 0) errors.baseDailyRate = 'Rate must be > 0';
    if (form.quantityAvailable < 0) errors.quantityAvailable = 'Must be ≥ 0';
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Save vehicle
  const handleSave = async () => {
    if (!validate()) return;
    setSaving(true);
    try {
      const payload = {
        ...form,
        baseDailyRate: parseFloat(form.baseDailyRate),
        year: parseInt(form.year),
        quantityAvailable: parseInt(form.quantityAvailable),
        contractExpiryDate: form.contractExpiryDate || null,
        imageUrl: form.imageUrl || null,
      };

      if (editingId) {
        await updateVehicle(editingId, payload);
        showToast('Vehicle updated successfully');
      } else {
        await addVehicle(payload);
        showToast('Vehicle added successfully');
      }
      setModalOpen(false);
      fetchVehicles();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to save vehicle';
      showToast(msg, 'error');
    } finally {
      setSaving(false);
    }
  };

  // Delete vehicle
  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteVehicle(deleteTarget.id);
      showToast('Vehicle deleted successfully');
      setDeleteTarget(null);
      fetchVehicles();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to delete vehicle';
      showToast(msg, 'error');
    } finally {
      setDeleting(false);
    }
  };

  // Clear filters
  const clearFilters = () => {
    setFilterName('');
    setFilterType('');
    setFilterQty('');
    setFilterPickup('');
    setFilterDays('');
  };

  const hasFilters = filterName || filterType || filterQty || filterPickup || filterDays;

  return (
    <div className="page-container" id="vehicles-page">
      {/* Page Header */}
      <div className="page-header">
        <h1 className="page-title">Vehicles</h1>
        <button className="btn btn-primary" onClick={openAdd} id="btn-add-vehicle">
          <FiPlus /> Add Vehicle
        </button>
      </div>

      {/* Filter Bar */}
      <div className="filter-bar" id="vehicle-filter-bar">
        <div className="filter-group">
          <span className="filter-label">Name (Make/Model)</span>
          <input
            type="text"
            placeholder="e.g. Hyundai Santa Fe"
            value={filterName}
            onChange={(e) => setFilterName(e.target.value)}
            id="filter-name"
          />
        </div>
        <div className="filter-group">
          <span className="filter-label">Vehicle Type</span>
          <select value={filterType} onChange={(e) => setFilterType(e.target.value)} id="filter-type">
            <option value="">All Types</option>
            {VEHICLE_TYPES.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>
        <div className="filter-group" style={{ minWidth: 100, flex: '0 0 100px' }}>
          <span className="filter-label">Min Qty</span>
          <input
            type="number"
            placeholder="0"
            min="0"
            value={filterQty}
            onChange={(e) => setFilterQty(e.target.value)}
            id="filter-qty"
          />
        </div>
        <div className="filter-group" style={{ minWidth: 150, flex: '0 0 150px' }}>
          <span className="filter-label">Pickup Date</span>
          <input
            type="date"
            value={filterPickup}
            onChange={(e) => setFilterPickup(e.target.value)}
            id="filter-pickup"
          />
        </div>
        <div className="filter-group" style={{ minWidth: 100, flex: '0 0 100px' }}>
          <span className="filter-label">Rental Days</span>
          <input
            type="number"
            placeholder="Days"
            min="1"
            value={filterDays}
            onChange={(e) => setFilterDays(e.target.value)}
            id="filter-days"
          />
        </div>
        <div className="filter-actions">
          <button className="btn btn-primary btn-sm" onClick={fetchVehicles} id="btn-search">
            <FiSearch /> Search
          </button>
          {hasFilters && (
            <button className="btn btn-ghost btn-sm" onClick={clearFilters} id="btn-clear-filters">
              <FiX /> Clear
            </button>
          )}
        </div>
      </div>

      {/* Vehicle List */}
      {loading ? (
        <div className="loading-spinner">
          <div className="spinner" />
        </div>
      ) : vehicles.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon"><FiTruck /></div>
          <div className="empty-state-text">No vehicles found</div>
          <div className="empty-state-subtext">
            {hasFilters ? 'Try adjusting your filters' : 'Add your first vehicle to get started'}
          </div>
        </div>
      ) : (
        sortedTypes.map((type) => (
          <div key={type} className="vehicle-type-section">
            <h2 className="vehicle-type-title">{type}</h2>
            <div className="vehicle-grid">
              {groupedVehicles[type].map((vehicle) => (
                <div key={vehicle.id} className="vehicle-card" id={`vehicle-card-${vehicle.id}`}>
                  <div className="vehicle-card-badge">{vehicle.type}</div>
                  {vehicle.imageUrl ? (
                    <img
                      className="vehicle-card-image"
                      src={vehicle.imageUrl}
                      alt={`${vehicle.make} ${vehicle.model}`}
                      onError={(e) => {
                        e.target.style.display = 'none';
                        e.target.nextSibling && (e.target.nextSibling.style.display = 'flex');
                      }}
                    />
                  ) : null}
                  <div
                    className="vehicle-card-image-placeholder"
                    style={vehicle.imageUrl ? { display: 'none' } : {}}
                  >
                    <FiImage />
                  </div>
                  <div className="vehicle-card-body">
                    <div className="vehicle-card-name">
                      {vehicle.make} {vehicle.model}
                    </div>
                    <div className="vehicle-card-details">
                      <div className="vehicle-card-detail">
                        <span className="vehicle-card-detail-icon"><FiCalendar /></span>
                        Year: <strong>{vehicle.year}</strong>
                      </div>
                      <div className="vehicle-card-detail">
                        <span className="vehicle-card-detail-icon"><FiDollarSign /></span>
                        Daily Rate: <strong>${parseFloat(vehicle.baseDailyRate).toFixed(2)}</strong>
                      </div>
                      <div className="vehicle-card-detail">
                        <span className="vehicle-card-detail-icon"><FiHash /></span>
                        Available: <strong>{vehicle.quantityAvailable}</strong>
                      </div>
                      {vehicle.contractExpiryDate && (
                        <div className="vehicle-card-detail">
                          <span className="vehicle-card-detail-icon"><FiCalendar /></span>
                          Contract Expires: <strong>{vehicle.contractExpiryDate}</strong>
                        </div>
                      )}
                    </div>
                    <div className="vehicle-card-actions">
                      <button
                        className="btn btn-primary btn-sm"
                        onClick={() => openEdit(vehicle)}
                        id={`btn-edit-${vehicle.id}`}
                      >
                        <FiEdit2 /> Edit
                      </button>
                      <button
                        className="btn-icon danger"
                        onClick={() => setDeleteTarget(vehicle)}
                        id={`btn-delete-${vehicle.id}`}
                        title="Delete vehicle"
                      >
                        <FiTrash2 />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))
      )}

      {/* Add/Edit Modal */}
      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} id="vehicle-modal">
            <div className="modal-header">
              <h3 className="modal-title">
                {editingId ? 'Edit Vehicle' : 'Add New Vehicle'}
              </h3>
              <button className="modal-close" onClick={() => setModalOpen(false)}><FiX /></button>
            </div>
            <div className="modal-body">
              <div className="form-grid">
                <div className="form-group">
                  <label className="form-label" htmlFor="form-make">Make *</label>
                  <input
                    id="form-make"
                    type="text"
                    placeholder="e.g. Hyundai"
                    value={form.make}
                    onChange={(e) => setForm({ ...form, make: e.target.value })}
                  />
                  {formErrors.make && <span className="form-error">{formErrors.make}</span>}
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="form-model">Model *</label>
                  <input
                    id="form-model"
                    type="text"
                    placeholder="e.g. Santa Fe"
                    value={form.model}
                    onChange={(e) => setForm({ ...form, model: e.target.value })}
                  />
                  {formErrors.model && <span className="form-error">{formErrors.model}</span>}
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="form-year">Year *</label>
                  <input
                    id="form-year"
                    type="number"
                    min="1900"
                    max="2099"
                    value={form.year}
                    onChange={(e) => setForm({ ...form, year: e.target.value })}
                  />
                  {formErrors.year && <span className="form-error">{formErrors.year}</span>}
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="form-type">Type *</label>
                  <select
                    id="form-type"
                    value={form.type}
                    onChange={(e) => setForm({ ...form, type: e.target.value })}
                  >
                    {VEHICLE_TYPES.map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="form-rate">Daily Rate ($) *</label>
                  <input
                    id="form-rate"
                    type="number"
                    step="0.01"
                    min="0.01"
                    placeholder="e.g. 75.00"
                    value={form.baseDailyRate}
                    onChange={(e) => setForm({ ...form, baseDailyRate: e.target.value })}
                  />
                  {formErrors.baseDailyRate && <span className="form-error">{formErrors.baseDailyRate}</span>}
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="form-qty">Quantity *</label>
                  <input
                    id="form-qty"
                    type="number"
                    min="0"
                    value={form.quantityAvailable}
                    onChange={(e) => setForm({ ...form, quantityAvailable: e.target.value })}
                  />
                  {formErrors.quantityAvailable && <span className="form-error">{formErrors.quantityAvailable}</span>}
                </div>
                <div className="form-group full-width">
                  <label className="form-label" htmlFor="form-image">Image URL</label>
                  <input
                    id="form-image"
                    type="url"
                    placeholder="https://example.com/image.jpg"
                    value={form.imageUrl}
                    onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                  />
                </div>
                <div className="form-group full-width">
                  <label className="form-label" htmlFor="form-expiry">Contract Expiry Date</label>
                  <input
                    id="form-expiry"
                    type="date"
                    value={form.contractExpiryDate}
                    onChange={(e) => setForm({ ...form, contractExpiryDate: e.target.value })}
                  />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setModalOpen(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSave} disabled={saving} id="btn-save-vehicle">
                {saving ? 'Saving…' : (editingId ? 'Update Vehicle' : 'Add Vehicle')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteTarget && (
        <div className="modal-overlay" onClick={() => setDeleteTarget(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 420 }} id="delete-confirm-modal">
            <div className="modal-header">
              <h3 className="modal-title">Confirm Delete</h3>
              <button className="modal-close" onClick={() => setDeleteTarget(null)}><FiX /></button>
            </div>
            <div className="modal-body">
              <p className="confirm-text">
                Are you sure you want to delete <strong>{deleteTarget.make} {deleteTarget.model}</strong>?
                <br />This action cannot be undone.
              </p>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setDeleteTarget(null)}>Cancel</button>
              <button className="btn btn-danger" onClick={handleDelete} disabled={deleting} id="btn-confirm-delete">
                {deleting ? 'Deleting…' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toasts */}
      {toasts.length > 0 && (
        <div className="toast-container">
          {toasts.map((t) => (
            <div key={t.id} className={`toast ${t.type}`}>
              <span className="toast-icon">
                {t.type === 'success' ? <FiCheckCircle /> : <FiAlertCircle />}
              </span>
              {t.message}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
