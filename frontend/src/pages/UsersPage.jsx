import { useState, useEffect } from 'react';
import { getUsers, deactivateUser, register } from '../api';
import {
  FiUserPlus, FiUserX, FiCheckCircle, FiAlertCircle,
  FiUsers, FiX,
} from 'react-icons/fi';

export default function UsersPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toasts, setToasts] = useState([]);

  // Register form
  const [regForm, setRegForm] = useState({
    username: '',
    password: '',
    email: '',
    role: 'SUPPORT',
  });
  const [regErrors, setRegErrors] = useState({});
  const [registering, setRegistering] = useState(false);

  // Deactivate confirm
  const [deactivateTarget, setDeactivateTarget] = useState(null);
  const [deactivating, setDeactivating] = useState(false);

  const showToast = (message, type = 'success') => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 4000);
  };

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await getUsers();
      setUsers(res.data);
    } catch (err) {
      console.error('Failed to fetch users:', err);
      showToast('Failed to load users', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  // Register user
  const validateRegister = () => {
    const errors = {};
    if (!regForm.username.trim() || regForm.username.length < 3)
      errors.username = 'Min 3 characters';
    if (!regForm.password.trim() || regForm.password.length < 6)
      errors.password = 'Min 6 characters';
    if (!regForm.email.trim() || !regForm.email.includes('@'))
      errors.email = 'Valid email required';
    setRegErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    if (!validateRegister()) return;
    setRegistering(true);
    try {
      await register(regForm);
      showToast(`User "${regForm.username}" registered successfully`);
      setRegForm({ username: '', password: '', email: '', role: 'SUPPORT' });
      setRegErrors({});
      fetchUsers();
    } catch (err) {
      const msg = err.response?.data?.message || 'Registration failed';
      showToast(msg, 'error');
    } finally {
      setRegistering(false);
    }
  };

  // Deactivate user
  const handleDeactivate = async () => {
    if (!deactivateTarget) return;
    setDeactivating(true);
    try {
      await deactivateUser(deactivateTarget.id);
      showToast(`User "${deactivateTarget.username}" deactivated`);
      setDeactivateTarget(null);
      fetchUsers();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to deactivate user';
      showToast(msg, 'error');
    } finally {
      setDeactivating(false);
    }
  };

  return (
    <div className="page-container" id="users-page">
      <div className="page-header">
        <h1 className="page-title">Users</h1>
      </div>

      {/* Register New User */}
      <div className="register-section" id="register-section">
        <h3 className="register-section-title">
          <FiUserPlus style={{ marginRight: 8, verticalAlign: 'middle' }} />
          Register New User
        </h3>
        <form className="register-form" onSubmit={handleRegister} id="register-form">
          <div className="form-group">
            <label className="form-label" htmlFor="reg-username">Username</label>
            <input
              id="reg-username"
              type="text"
              placeholder="johndoe"
              value={regForm.username}
              onChange={(e) => setRegForm({ ...regForm, username: e.target.value })}
            />
            {regErrors.username && <span className="form-error">{regErrors.username}</span>}
          </div>
          <div className="form-group">
            <label className="form-label" htmlFor="reg-email">Email</label>
            <input
              id="reg-email"
              type="email"
              placeholder="john@email.com"
              value={regForm.email}
              onChange={(e) => setRegForm({ ...regForm, email: e.target.value })}
            />
            {regErrors.email && <span className="form-error">{regErrors.email}</span>}
          </div>
          <div className="form-group">
            <label className="form-label" htmlFor="reg-password">Password</label>
            <input
              id="reg-password"
              type="password"
              placeholder="Min 6 characters"
              value={regForm.password}
              onChange={(e) => setRegForm({ ...regForm, password: e.target.value })}
            />
            {regErrors.password && <span className="form-error">{regErrors.password}</span>}
          </div>
          <div className="form-group">
            <label className="form-label" htmlFor="reg-role">Role</label>
            <select
              id="reg-role"
              value={regForm.role}
              onChange={(e) => setRegForm({ ...regForm, role: e.target.value })}
            >
              <option value="SUPPORT">Support</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={registering}
            id="btn-register"
          >
            <FiUserPlus />
            {registering ? 'Registering…' : 'Register'}
          </button>
        </form>
      </div>

      {/* Users Table */}
      {loading ? (
        <div className="loading-spinner">
          <div className="spinner" />
        </div>
      ) : users.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon"><FiUsers /></div>
          <div className="empty-state-text">No support users found</div>
          <div className="empty-state-subtext">Register a new user above to get started</div>
        </div>
      ) : (
        <div className="data-table-wrapper" id="users-table">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Email</th>
                <th>Contact</th>
                <th>Role</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.id}</td>
                  <td style={{ fontWeight: 600 }}>{user.username}</td>
                  <td>{user.email}</td>
                  <td>{user.contactNumber || '—'}</td>
                  <td>
                    <span style={{
                      fontWeight: 600,
                      color: user.role === 'ADMIN' ? 'var(--primary)' : 'var(--accent)',
                      fontSize: '0.8rem',
                      textTransform: 'uppercase',
                    }}>
                      {user.role}
                    </span>
                  </td>
                  <td>
                    <span className={`status-badge ${user.active ? 'active' : 'inactive'}`}>
                      <span className="status-dot"></span>
                      {user.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    {user.active && (
                      <button
                        className="btn btn-danger btn-sm"
                        onClick={() => setDeactivateTarget(user)}
                        id={`btn-deactivate-${user.id}`}
                        title="Deactivate user"
                      >
                        <FiUserX /> Deactivate
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Deactivate Confirmation Modal */}
      {deactivateTarget && (
        <div className="modal-overlay" onClick={() => setDeactivateTarget(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 420 }} id="deactivate-confirm-modal">
            <div className="modal-header">
              <h3 className="modal-title">Deactivate User</h3>
              <button className="modal-close" onClick={() => setDeactivateTarget(null)}><FiX /></button>
            </div>
            <div className="modal-body">
              <p className="confirm-text">
                Are you sure you want to deactivate <strong>{deactivateTarget.username}</strong>?
                <br />They will no longer be able to log in.
              </p>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setDeactivateTarget(null)}>Cancel</button>
              <button className="btn btn-danger" onClick={handleDeactivate} disabled={deactivating} id="btn-confirm-deactivate">
                {deactivating ? 'Deactivating…' : 'Deactivate'}
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
