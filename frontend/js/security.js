// ===== MAISON Security Guard =====
// Applies right-click and DevTools blocking based on server-side config.
// Only active for non-admin users. Admins are never affected.
(function () {
  'use strict';

  // Don't run on admin pages
  if (window.location.pathname.includes('/admin/')) return;

  let cfg = { disableRightClick: false, disableDevtoolsKey: false, autoBanDevtools: false };
  let devtoolsOpen = false;
  let banReported = false;

  // --------------------------------------------------
  // Load config from server (best-effort, non-blocking)
  // --------------------------------------------------
  async function loadConfig() {
    try {
      const res = await fetch((window.location.hostname === 'localhost'
        ? 'http://localhost:8080/api'
        : '/api') + '/public/security-config');
      if (!res.ok) return;
      const json = await res.json();
      if (json.success && json.data) {
        cfg = json.data;
        applyPolicies();
      }
    } catch { /* silently ignore */ }
  }

  // --------------------------------------------------
  // Check if current user is admin — never block admins
  // --------------------------------------------------
  function isAdmin() {
    try {
      const u = JSON.parse(localStorage.getItem('userInfo') || 'null');
      return u && (u.role === 'ROLE_ADMIN' || u.role === 'ADMIN');
    } catch { return false; }
  }

  // --------------------------------------------------
  // Apply right-click block
  // --------------------------------------------------
  function applyRightClickBlock() {
    document.addEventListener('contextmenu', function handler(e) {
      if (isAdmin()) return; // admins always pass
      e.preventDefault();
      e.stopPropagation();
      return false;
    }, true);
  }

  // --------------------------------------------------
  // Apply keyboard shortcut block (F12, Ctrl+Shift+I/J/C/U)
  // --------------------------------------------------
  function applyKeyBlock() {
    document.addEventListener('keydown', function handler(e) {
      if (isAdmin()) return;
      const ctrl = e.ctrlKey || e.metaKey;
      const blocked =
        e.key === 'F12' ||
        (ctrl && e.shiftKey && ['I', 'J', 'C'].includes(e.key.toUpperCase())) ||
        (ctrl && e.key.toUpperCase() === 'U');
      if (blocked) {
        e.preventDefault();
        e.stopPropagation();
        return false;
      }
    }, true);
  }

  // --------------------------------------------------
  // DevTools detection (size-based heuristic)
  // Fires a callback when DevTools appears to be open.
  // --------------------------------------------------
  function startDevtoolsDetection() {
    const THRESHOLD = 160;

    function check() {
      const widthDiff  = window.outerWidth  - window.innerWidth  > THRESHOLD;
      const heightDiff = window.outerHeight - window.innerHeight > THRESHOLD;
      const nowOpen = widthDiff || heightDiff;

      if (nowOpen && !devtoolsOpen) {
        devtoolsOpen = true;
        onDevtoolsOpened();
      } else if (!nowOpen && devtoolsOpen) {
        devtoolsOpen = false;
      }
    }

    setInterval(check, 1000);
    window.addEventListener('resize', check);
  }

  // --------------------------------------------------
  // Called when DevTools is detected
  // --------------------------------------------------
  async function onDevtoolsOpened() {
    if (isAdmin()) return; // admins are exempt

    if (cfg.autoBanDevtools && !banReported) {
      banReported = true;
      const token = localStorage.getItem('accessToken');
      if (token && token !== 'undefined' && token !== 'null') {
        try {
          const base = window.location.hostname === 'localhost'
            ? 'http://localhost:8080/api'
            : '/api';
          const res = await fetch(base + '/security/devtools-detected', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': 'Bearer ' + token
            }
          });
          const json = await res.json().catch(() => ({}));
          if (json.data === 'account_suspended') {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('userInfo');
            window.location.href = '/banned.html?reason=devtools';
            return;
          }
        } catch { /* ignore */ }
      }
    }
  }

  // --------------------------------------------------
  // Activate based on loaded config
  // --------------------------------------------------
  function applyPolicies() {
    if (cfg.disableRightClick)  applyRightClickBlock();
    if (cfg.disableDevtoolsKey) applyKeyBlock();
    if (cfg.autoBanDevtools)    startDevtoolsDetection();
  }

  // Load config immediately; policies applied after
  loadConfig();
})();
