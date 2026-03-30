// ===== MAISON Security Guard =====
// Blocks right-click and DevTools shortcuts immediately (before server config loads).
// Admin users are always exempt. Server config can disable features.
(function () {
  'use strict';

  // Don't run on admin pages
  if (window.location.pathname.includes('/admin/')) return;

  // --------------------------------------------------
  // Check if current user is admin — admins always pass
  // --------------------------------------------------
  function isAdmin() {
    try {
      const u = JSON.parse(localStorage.getItem('userInfo') || 'null');
      return u && (u.role === 'ROLE_ADMIN' || u.role === 'ADMIN');
    } catch { return false; }
  }

  // --------------------------------------------------
  // State — active by default, disabled if server says so
  // --------------------------------------------------
  let rightClickBlocked  = true;
  let devtoolsKeyBlocked = true;
  let autoBanEnabled     = false;
  let devtoolsOpen       = false;
  let banReported        = false;

  // --------------------------------------------------
  // Right-click handler (always registered, checks flag)
  // --------------------------------------------------
  document.addEventListener('contextmenu', function (e) {
    if (!rightClickBlocked || isAdmin()) return;
    e.preventDefault();
    e.stopPropagation();
  }, true);

  // --------------------------------------------------
  // Keyboard handler (always registered, checks flag)
  // --------------------------------------------------
  document.addEventListener('keydown', function (e) {
    if (!devtoolsKeyBlocked || isAdmin()) return;
    const ctrl = e.ctrlKey || e.metaKey;
    const blocked =
      e.key === 'F12' ||
      (ctrl && e.shiftKey && ['I', 'J', 'C'].includes(e.key.toUpperCase())) ||
      (ctrl && e.key.toUpperCase() === 'U');
    if (blocked) {
      e.preventDefault();
      e.stopPropagation();
    }
  }, true);

  // --------------------------------------------------
  // DevTools size-based detection
  // --------------------------------------------------
  function checkDevtools() {
    if (!autoBanEnabled || isAdmin()) return;
    const THRESHOLD = 160;
    const nowOpen = (window.outerWidth - window.innerWidth > THRESHOLD) ||
                    (window.outerHeight - window.innerHeight > THRESHOLD);
    if (nowOpen && !devtoolsOpen) {
      devtoolsOpen = true;
      onDevtoolsOpened();
    } else if (!nowOpen) {
      devtoolsOpen = false;
    }
  }

  async function onDevtoolsOpened() {
    if (banReported) return;
    banReported = true;
    const token = localStorage.getItem('accessToken');
    if (!token || token === 'undefined' || token === 'null') return;
    try {
      const base = window.location.hostname === 'localhost'
        ? 'http://localhost:8080/api' : '/api';
      const res = await fetch(base + '/security/devtools-detected', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token }
      });
      const json = await res.json().catch(() => ({}));
      if (json.data === 'account_suspended') {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
        window.location.href = '/banned.html?reason=devtools';
      }
    } catch { /* ignore */ }
  }

  // --------------------------------------------------
  // Load server config and update flags
  // Policies are already active with defaults above;
  // only disable them if server explicitly says false.
  // --------------------------------------------------
  async function loadConfig() {
    try {
      const base = window.location.hostname === 'localhost'
        ? 'http://localhost:8080/api' : '/api';
      const res = await fetch(base + '/public/security-config');
      if (!res.ok) return;
      const json = await res.json();
      if (!json.success || !json.data) return;
      const cfg = json.data;
      rightClickBlocked  = cfg.disableRightClick  !== false;
      devtoolsKeyBlocked = cfg.disableDevtoolsKey !== false;
      autoBanEnabled     = cfg.autoBanDevtools     === true;
      if (autoBanEnabled) {
        setInterval(checkDevtools, 1000);
        window.addEventListener('resize', checkDevtools);
      }
    } catch { /* keep defaults */ }
  }

  loadConfig();
})();
