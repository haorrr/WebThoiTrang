// ===== MAISON Security Guard =====
(function () {
  'use strict';

  if (window.location.pathname.includes('/admin/')) return;

  function isAdmin() {
    try {
      const u = JSON.parse(localStorage.getItem('userInfo') || 'null');
      return u && (u.role === 'ROLE_ADMIN' || u.role === 'ADMIN');
    } catch { return false; }
  }

  // Flags — block by default, updated by server config
  let rightClickBlocked  = true;
  let devtoolsKeyBlocked = true;
  let autoBanEnabled     = false;
  let banReported        = false;

  // ── Overlay (shown when DevTools detected) ─────────────────────────────────
  let overlay = null;

  function showDevtoolsOverlay() {
    if (overlay) return;
    overlay = document.createElement('div');
    overlay.id = '__sec_overlay';
    overlay.style.cssText = [
      'position:fixed', 'inset:0', 'z-index:2147483647',
      'background:rgba(12,10,9,0.97)',
      'display:flex', 'flex-direction:column',
      'align-items:center', 'justify-content:center',
      'font-family:Inter,sans-serif', 'color:#fff',
      'user-select:none',
    ].join(';');

    overlay.innerHTML = `
      <div style="text-align:center;padding:2rem;max-width:420px">
        <div style="width:72px;height:72px;background:#7F1D1D;border-radius:50%;
                    display:flex;align-items:center;justify-content:center;margin:0 auto 1.5rem">
          <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 24 24"
               fill="none" stroke="#FCA5A5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <div style="font-size:1.4rem;font-weight:700;letter-spacing:0.05em;margin-bottom:0.75rem">
          TRUY CẬP BỊ CHẶN
        </div>
        <div style="font-size:0.95rem;color:rgba(255,255,255,0.6);line-height:1.7;margin-bottom:1.5rem">
          Công cụ dành cho nhà phát triển<br>
          <strong style="color:#FCA5A5">không được phép</strong> sử dụng trên trang này.<br>
          Vui lòng đóng DevTools để tiếp tục.
        </div>
        <div style="font-size:0.75rem;color:rgba(255,255,255,0.25);letter-spacing:0.15em">
          MAISON. SECURITY
        </div>
      </div>`;

    document.body.appendChild(overlay);
    // Prevent scrolling behind overlay
    document.documentElement.style.overflow = 'hidden';
  }

  function hideDevtoolsOverlay() {
    if (!overlay) return;
    overlay.remove();
    overlay = null;
    document.documentElement.style.overflow = '';
  }

  // ── DevTools detection (size heuristic + debugger trick) ───────────────────
  let devtoolsOpen = false;

  function checkDevtools() {
    if (isAdmin()) return;
    const THRESHOLD = 160;
    const bySize =
      (window.outerWidth  - window.innerWidth  > THRESHOLD) ||
      (window.outerHeight - window.innerHeight > THRESHOLD);

    if (bySize && !devtoolsOpen) {
      devtoolsOpen = true;
      showDevtoolsOverlay();
      if (autoBanEnabled) reportBan();
    } else if (!bySize && devtoolsOpen) {
      devtoolsOpen = false;
      hideDevtoolsOverlay();
    }
  }

  // ── Auto-ban (logged-in users only) ────────────────────────────────────────
  async function reportBan() {
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
    } catch { banReported = false; }
  }

  // ── Right-click block ───────────────────────────────────────────────────────
  document.addEventListener('contextmenu', function (e) {
    if (!rightClickBlocked || isAdmin()) return;
    e.preventDefault();
    e.stopPropagation();
  }, true);

  // ── Keyboard block ──────────────────────────────────────────────────────────
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

  // ── Start detection loop ────────────────────────────────────────────────────
  setInterval(checkDevtools, 800);
  window.addEventListener('resize', checkDevtools);

  // ── Load server config (update flags, keep defaults until loaded) ───────────
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
    } catch { /* keep defaults */ }
  }

  loadConfig();
})();
