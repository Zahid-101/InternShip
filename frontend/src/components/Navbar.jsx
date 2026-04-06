import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { useState, useRef, useEffect } from 'react';
import { FiLogOut, FiUser, FiChevronDown } from 'react-icons/fi';

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initials = user?.username
    ? user.username.slice(0, 2).toUpperCase()
    : 'U';

  return (
    <nav className="navbar" id="main-navbar">
      <div className="navbar-left">
        {/* DriveEase Logo */}
        <NavLink to="/vehicles" className="navbar-logo">
          <div className="navbar-logo-icon">
            {/* Car icon */}
            <svg viewBox="0 0 24 24" fill="none" stroke="#22577A" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M5 17h14M5 17a2 2 0 01-2-2V9a2 2 0 012-2h1l2-3h8l2 3h1a2 2 0 012 2v6a2 2 0 01-2 2M5 17a2 2 0 100 4 2 2 0 000-4zM19 17a2 2 0 100 4 2 2 0 000-4z"/>
            </svg>
            {/* Mileage dot */}
            <span className="navbar-logo-mileage">
              <svg viewBox="0 0 12 12" fill="none">
                <path d="M2 8L6 3L10 8" stroke="#FFFFFF" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </span>
          </div>
          <span className="navbar-logo-text">Drive<br/>Ease</span>
        </NavLink>

        {/* Navigation Links */}
        <ul className="navbar-links">
          <li>
            <NavLink to="/vehicles" className={({ isActive }) => isActive ? 'active' : ''} id="nav-vehicles">
              Vehicles
            </NavLink>
          </li>
          {isAdmin && (
            <li>
              <NavLink to="/users" className={({ isActive }) => isActive ? 'active' : ''} id="nav-users">
                Users
              </NavLink>
            </li>
          )}
        </ul>
      </div>

      <div className="navbar-right">
        <div className="navbar-user" ref={dropdownRef} onClick={() => setDropdownOpen(!dropdownOpen)} id="navbar-user-menu">
          <div className="navbar-avatar">{initials}</div>
          <FiChevronDown style={{ color: 'rgba(255,255,255,0.7)', fontSize: '0.9rem', transition: 'transform 0.2s', transform: dropdownOpen ? 'rotate(180deg)' : 'rotate(0)' }} />

          {dropdownOpen && (
            <div className="navbar-dropdown">
              <div className="navbar-dropdown-item" style={{ pointerEvents: 'none' }}>
                <FiUser />
                <div>
                  <div style={{ fontWeight: 600 }}>{user?.username}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{user?.role}</div>
                </div>
              </div>
              <div className="navbar-dropdown-divider"></div>
              <button className="navbar-dropdown-item danger" onClick={handleLogout} id="btn-logout">
                <FiLogOut />
                Sign Out
              </button>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
