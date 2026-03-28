// ===== API CLIENT — FashionShop =====
const API_BASE = window.location.hostname === 'localhost' ? 'http://localhost:8080/api' : '/api';

// --- Token Management ---
const Auth = {
  getAccess: () => localStorage.getItem('accessToken'),
  getRefresh: () => localStorage.getItem('refreshToken'),
  getUser: () => {
    try { return JSON.parse(localStorage.getItem('userInfo')); } catch { return null; }
  },
  save: (accessToken, refreshToken, userInfo) => {
    if (accessToken) localStorage.setItem('accessToken', accessToken);
    if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
    if (userInfo) localStorage.setItem('userInfo', JSON.stringify(userInfo));
  },
  clear: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userInfo');
  },
  isLoggedIn: () => {
    const token = localStorage.getItem('accessToken');
    return !!token && token !== 'undefined' && token !== 'null';
  },
  isAdmin: () => {
    const u = Auth.getUser();
    return u && (u.role === 'ROLE_ADMIN' || u.role === 'ADMIN');
  }
};

// --- VND Formatter ---
function formatVND(amount) {
  if (amount == null) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString('vi-VN', { dateStyle: 'medium', timeStyle: 'short' });
}

// --- Core Fetch Wrapper ---
let _refreshing = null;

async function apiFetch(endpoint, options = {}) {
  const url = API_BASE + endpoint;
  const token = Auth.getAccess();

  const headers = { 'Content-Type': 'application/json', ...options.headers };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  let res = await fetch(url, { ...options, headers });

  // Auto-refresh on 401
  if (res.status === 401 && Auth.getRefresh()) {
    if (!_refreshing) {
      _refreshing = fetch(API_BASE + '/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: Auth.getRefresh() })
      }).then(r => r.json()).then(data => {
        if (data.success && data.data) {
          Auth.save(data.data.accessToken, data.data.refreshToken, null);
        } else {
          Auth.clear();
          window.location.href = '/login.html';
        }
        _refreshing = null;
      }).catch(() => { Auth.clear(); window.location.href = '/login.html'; _refreshing = null; });
    }
    await _refreshing;
    const newToken = Auth.getAccess();
    if (newToken) headers['Authorization'] = `Bearer ${newToken}`;
    res = await fetch(url, { ...options, headers });
  }

  const data = await res.json().catch(() => ({}));
  if (!res.ok && !data.success) {
    throw { status: res.status, message: data.message || 'Lỗi hệ thống', errors: data.errors };
  }
  return data;
}

// ===== API HELPERS =====
const api = {
  auth: {
    login: (email, password) => apiFetch('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
    register: (name, email, password, referralCode) => apiFetch('/auth/register', { method: 'POST', body: JSON.stringify({ name, email, password, referralCode: referralCode || null }) }),
    logout: () => apiFetch('/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken: Auth.getRefresh() }) }),
    forgotPassword: (email) => apiFetch('/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),
    verifyResetToken: (token) => apiFetch(`/auth/verify-reset-token?token=${token}`),
    resetPassword: (token, newPassword) => apiFetch('/auth/reset-password', { method: 'POST', body: JSON.stringify({ token, newPassword }) }),
  },
  products: {
    list: (params = {}) => apiFetch(`/products?${new URLSearchParams(params)}`),
    get: (id) => apiFetch(`/products/${id}`),
    getBySlug: (slug) => apiFetch(`/products/slug/${slug}`),
    create: (data) => apiFetch('/products', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => apiFetch(`/products/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => apiFetch(`/products/${id}`, { method: 'DELETE' }),
    toggleStatus: (id) => apiFetch(`/products/${id}/status`, { method: 'PATCH' }),
    uploadImage: (id, formData) => {
      const token = Auth.getAccess();
      return fetch(`${API_BASE}/products/${id}/images`, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }, body: formData }).then(r => r.json());
    },
    addImageByUrl: (id, imageUrl, isPrimary = false) => apiFetch(`/products/${id}/images/url?imageUrl=${encodeURIComponent(imageUrl)}&isPrimary=${isPrimary}`, { method: 'POST' }),
    deleteImage: (id, imageId) => apiFetch(`/products/${id}/images/${imageId}`, { method: 'DELETE' }),
    generateAiDescription: (id) => apiFetch(`/products/${id}/ai-description`, { method: 'POST' }),
  },
  categories: {
    list: () => apiFetch('/categories'),
    get: (id) => apiFetch(`/categories/${id}`),
    create: (data) => apiFetch('/categories', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => apiFetch(`/categories/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => apiFetch(`/categories/${id}`, { method: 'DELETE' }),
    toggleStatus: (id) => apiFetch(`/categories/${id}/status`, { method: 'PATCH' }),
  },
  variants: {
    list: (productId) => apiFetch(`/products/${productId}/variants`),
    create: (productId, data) => apiFetch(`/products/${productId}/variants`, { method: 'POST', body: JSON.stringify(data) }),
    update: (productId, variantId, data) => apiFetch(`/products/${productId}/variants/${variantId}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (productId, variantId) => apiFetch(`/products/${productId}/variants/${variantId}`, { method: 'DELETE' }),
    adjustStock: (productId, variantId, quantity) => apiFetch(`/products/${productId}/variants/${variantId}/stock`, { method: 'PATCH', body: JSON.stringify({ quantity }) }),
  },
  cart: {
    get: () => apiFetch('/cart'),
    addItem: (productId, quantity, size, color, variantId) => apiFetch('/cart/items', { method: 'POST', body: JSON.stringify({ productId, quantity, size, color, variantId }) }),
    updateItem: (itemId, quantity) => apiFetch(`/cart/items/${itemId}`, { method: 'PUT', body: JSON.stringify({ quantity }) }),
    removeItem: (itemId) => apiFetch(`/cart/items/${itemId}`, { method: 'DELETE' }),
    clear: () => apiFetch('/cart', { method: 'DELETE' }),
  },
  orders: {
    create: (data) => apiFetch('/orders', { method: 'POST', body: JSON.stringify(data) }),
    list: (params = {}) => apiFetch(`/orders?${new URLSearchParams(params)}`),
    get: (id) => apiFetch(`/orders/${id}`),
    cancel: (id) => apiFetch(`/orders/${id}/cancel`, { method: 'POST' }),
    adminList: (params = {}) => apiFetch(`/orders/admin/all?${new URLSearchParams(params)}`),
    adminUpdateStatus: (id, data) => apiFetch(`/orders/admin/${id}/status`, { method: 'PATCH', body: JSON.stringify(data) }),
  },
  reviews: {
    getByProduct: (productId) => apiFetch(`/reviews/product/${productId}`),
    getMy: () => apiFetch('/reviews/my'),
    create: (data) => apiFetch('/reviews', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => apiFetch(`/reviews/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => apiFetch(`/reviews/${id}`, { method: 'DELETE' }),
    adminList: (params = {}) => apiFetch(`/reviews/admin/all?${new URLSearchParams(params)}`),
    adminModerate: (id, status) => apiFetch(`/reviews/admin/${id}/moderate`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  },
  coupons: {
    validate: (code, orderAmount) => apiFetch('/coupons/validate', { method: 'POST', body: JSON.stringify({ code, orderAmount }) }),
    list: () => apiFetch('/coupons'),
    get: (id) => apiFetch(`/coupons/${id}`),
    create: (data) => apiFetch('/coupons', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => apiFetch(`/coupons/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => apiFetch(`/coupons/${id}`, { method: 'DELETE' }),
    toggle: (id) => apiFetch(`/coupons/${id}/toggle`, { method: 'PATCH' }),
  },
  users: {
    list: (params = {}) => apiFetch(`/users?${new URLSearchParams(params)}`),
    me: () => apiFetch('/users/me'),
    updateMe: (data) => apiFetch('/users/me', { method: 'PUT', body: JSON.stringify(data) }),
    get: (id) => apiFetch(`/users/${id}`),
    update: (id, data) => apiFetch(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => apiFetch(`/users/${id}`, { method: 'DELETE' }),
    toggleStatus: (id) => apiFetch(`/users/${id}/status`, { method: 'PATCH' }),
  },
  dashboard: {
    get: () => apiFetch('/admin/dashboard'),
  },
  ai: {
    chat: (message) => apiFetch('/ai/chat', { method: 'POST', body: JSON.stringify({ message }) }),
    recommendations: (preferences, budget) => apiFetch(`/ai/recommendations?preferences=${encodeURIComponent(preferences)}&budget=${budget}`),
  },
  flashSales: {
    getActive: () => apiFetch('/flash-sales/active'),
    getById: (id) => apiFetch(`/flash-sales/${id}`),
    adminList: (params = {}) => apiFetch(`/admin/flash-sales?${new URLSearchParams(params)}`),
    create: (data) => apiFetch('/admin/flash-sales', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => apiFetch(`/admin/flash-sales/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => apiFetch(`/admin/flash-sales/${id}`, { method: 'DELETE' }),
    addProduct: (id, productId, stockLimit) => apiFetch(`/admin/flash-sales/${id}/products`, { method: 'POST', body: JSON.stringify({ productId, stockLimit }) }),
    removeProduct: (id, productId) => apiFetch(`/admin/flash-sales/${id}/products/${productId}`, { method: 'DELETE' }),
  },
  wishlist: {
    get: () => apiFetch('/wishlist'),
    toggle: (productId) => apiFetch(`/wishlist/${productId}`, { method: 'POST' }),
    check: (productId) => apiFetch(`/wishlist/check/${productId}`),
  },
  loyalty: {
    getSummary: () => apiFetch('/loyalty'),
    getReferralCode: () => apiFetch('/loyalty/referral-code'),
    adminListUsers: (params = {}) => apiFetch('/loyalty/admin/users?' + new URLSearchParams(params)),
    adminGetConfig: () => apiFetch('/loyalty/admin/config'),
    adminUpdateConfig: (data) => apiFetch('/loyalty/admin/config', { method: 'PUT', body: JSON.stringify(data) }),
    adminAdjustPoints: (userId, data) => apiFetch(`/loyalty/admin/adjust/${userId}`, { method: 'POST', body: JSON.stringify(data) }),
  },
  inventory: {
    getMovements: (productId, params = {}) => apiFetch(`/admin/inventory/products/${productId}/movements?${new URLSearchParams(params)}`),
    adjust: (data) => apiFetch('/admin/inventory/adjust', { method: 'POST', body: JSON.stringify(data) }),
  },
};

// ===== UI HELPERS =====
function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span>${message}</span>`;
  document.body.appendChild(toast);
  setTimeout(() => toast.classList.add('show'), 10);
  setTimeout(() => { toast.classList.remove('show'); setTimeout(() => toast.remove(), 300); }, 3500);
}

function setLoading(btn, loading) {
  if (loading) {
    btn.dataset.originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-sm"></span> Đang xử lý...`;
  } else {
    btn.disabled = false;
    btn.innerHTML = btn.dataset.originalText;
  }
}

function renderStars(rating) {
  return [1,2,3,4,5].map(i => `<svg class="star ${i <= rating ? 'filled' : ''}" viewBox="0 0 24 24" fill="${i <= rating ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>`).join('');
}

function getStatusBadge(status) {
  const map = {
    PENDING: ['badge-warning', 'Chờ xác nhận'],
    CONFIRMED: ['badge-info', 'Đã xác nhận'],
    SHIPPING: ['badge-primary', 'Đang giao'],
    DELIVERED: ['badge-success', 'Đã giao'],
    CANCELLED: ['badge-danger', 'Đã hủy'],
    ACTIVE: ['badge-success', 'Hoạt động'],
    INACTIVE: ['badge-danger', 'Vô hiệu'],
    APPROVED: ['badge-success', 'Đã duyệt'],
    REJECTED: ['badge-danger', 'Từ chối'],
    PENDING: ['badge-warning', 'Chờ duyệt'],
  };
  const [cls, label] = map[status] || ['badge-secondary', status];
  return `<span class="badge ${cls}">${label}</span>`;
}

async function updateCartCount() {
  if (!Auth.isLoggedIn()) return;
  try {
    const res = await api.cart.get();
    const count = res.data?.totalItems || 0;
    document.querySelectorAll('.cart-count').forEach(el => {
      el.textContent = count;
      el.style.display = count > 0 ? 'flex' : 'none';
    });
  } catch {}
}

// Render shared navbar dynamically based on auth state
function renderNavActions() {
  const user = Auth.getUser();
  const isLoggedIn = Auth.isLoggedIn();
  const isAdmin = Auth.isAdmin();
  const navActions = document.getElementById('navActions');
  if (!navActions) return;

  if (isLoggedIn) {
    navActions.innerHTML = `
      ${isAdmin ? `<a href="/admin/dashboard.html" class="nav-link admin-link">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
        Admin
      </a>` : ''}
      <a href="/wishlist.html" class="nav-link" title="Yêu thích">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
      </a>
      <a href="/profile.html" class="nav-link user-link">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        ${user?.name || 'Tài khoản'}
      </a>
      <a href="/cart.html" class="navbar__cart" id="cartBtn">
        <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
        <span class="navbar__cart-count cart-count" style="display:none">0</span>
      </a>
      <button onclick="handleLogout()" class="btn btn-outline btn-sm">Đăng xuất</button>
    `;
    updateCartCount();
  } else {
    navActions.innerHTML = `
      <a href="/login.html" class="btn btn-outline btn-sm">Đăng nhập</a>
      <a href="/register.html" class="btn btn-primary btn-sm">Đăng ký</a>
    `;
  }
}

async function handleLogout() {
  try { await api.auth.logout(); } catch {}
  Auth.clear();
  showToast('Đã đăng xuất!');
  setTimeout(() => window.location.href = '/index.html', 800);
}
