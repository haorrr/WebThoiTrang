# Phase 03 — Admin UI

## 3.1 `frontend/admin/dashboard.html` — Toxic Detection toggle

### Add to existing `SECURITY_KEYS` array (reuses existing render/save pattern)

```javascript
const SECURITY_KEYS = [
  // ... existing 3 entries ...
  {
    key: 'toxic_detection.enabled',
    label: '🛡️ Kiểm duyệt review toxic (PhoBERT AI)',
    desc: 'Tự động phát hiện và giữ PENDING các đánh giá toxic. Review sạch được duyệt ngay.'
  },
];
```

No new JS needed — `renderSecuritySettings()` and `saveSecuritySetting()` handle it automatically.

### Add threshold numeric input card below security settings card

```html
<div class="admin-card" style="margin-bottom:1.5rem">
  <div class="admin-card__header">
    <h2>Ngưỡng điểm toxic</h2>
    <span id="thresholdStatus" style="font-size:0.8rem;color:var(--color-text-muted)"></span>
  </div>
  <div class="admin-card__body" style="display:flex;align-items:center;gap:16px;padding:1rem">
    <span style="font-size:0.9rem">Score ≥</span>
    <input type="number" id="toxicThreshold" min="0" max="1" step="0.05"
           style="width:80px;padding:6px 10px;border:1px solid var(--color-border);border-radius:6px"
           onchange="saveThreshold(this.value)">
    <span style="font-size:0.85rem;color:var(--color-text-muted)">
      → giữ PENDING (mặc định: 0.70)
    </span>
  </div>
</div>
```

### Add to `loadSecuritySettings()` after building `map`:
```javascript
const t = map['toxic_detection.threshold'] || '0.70';
const el = document.getElementById('toxicThreshold');
if (el) el.value = t;
```

### New JS function:
```javascript
async function saveThreshold(val) {
  const s = document.getElementById('thresholdStatus');
  try {
    await api.config.update('toxic_detection.threshold', val);
    s.textContent = 'Đã lưu ✓'; s.style.color = '#16A34A';
    setTimeout(() => s.textContent = '', 2000);
  } catch { showToast('Không thể lưu ngưỡng', 'error'); }
}
```

---

## 3.2 `frontend/admin/reviews.html` — Toxic score column

### thead — insert `<th>Toxic</th>` before `<th>Trạng thái</th>`

```html
<tr>
  <th>Người dùng</th><th>Sản phẩm</th><th>Sao</th><th>Nhận xét</th>
  <th>Ảnh</th><th>Ngày</th>
  <th>Toxic</th>   <!-- NEW -->
  <th>Trạng thái</th><th>Thao tác</th>
</tr>
```

Update `colspan` in loading/empty/error rows: `7` → `8`.

### Row template — insert toxic cell after date `<td>`

```javascript
<td>${renderToxicBadge(r.toxicScore)}</td>
```

### New helper function

```javascript
function renderToxicBadge(score) {
  if (score == null) {
    return '<span style="font-size:0.75rem;color:var(--color-text-muted)">—</span>';
  }
  const pct = Math.round(score * 100);
  const color = pct >= 70 ? '#DC2626' : pct >= 40 ? '#D97706' : '#16A34A';
  const label = pct >= 70 ? '⚠ Toxic' : pct >= 40 ? '~ Nghi ngờ' : '✓ OK';
  return `
    <div style="text-align:center">
      <div style="font-size:0.72rem;font-weight:600;color:${color}">${label}</div>
      <div style="width:48px;height:4px;background:#E5E7EB;border-radius:2px;margin:2px auto;overflow:hidden">
        <div style="width:${pct}%;height:100%;background:${color}"></div>
      </div>
      <div style="font-size:0.68rem;color:var(--color-text-muted)">${pct}%</div>
    </div>`;
}
```

---

## 3.3 No new API endpoints needed

- `GET /api/reviews/admin/all` → `ReviewResponse` now includes `toxicScore` (Phase 02)
- `GET /api/admin/config` + `PUT /api/admin/config/{key}` → unchanged

---

## Checklist

- [ ] Toggle `toxic_detection.enabled` appears in security settings grid, saves correctly
- [ ] Threshold input pre-populated from system_config on page load
- [ ] `reviews.html` thead has Toxic column, colspan updated to 8
- [ ] Badge: `—` for null, green `✓ OK` <40%, amber `~ Nghi ngờ` 40-70%, red `⚠ Toxic` ≥70%
- [ ] Existing approve/reject buttons unaffected
