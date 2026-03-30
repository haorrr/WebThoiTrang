/**
 * ============================================================
 *  BULK ADD PRODUCTS — F12 Console Script
 *  FashionShop Admin Tool
 * ============================================================
 *
 *  CÁCH DÙNG:
 *  1. Đăng nhập admin trên Chrome
 *  2. Mở F12 → Console
 *  3. Copy & paste products-data.js vào console → Enter
 *  4. Copy & paste toàn bộ file này vào console → Enter
 *
 *  Script sẽ tự động:
 *  - Lấy JWT token từ localStorage
 *  - Tạo từng sản phẩm
 *  - Thêm ảnh theo URL
 *  - Thêm tất cả biến thể (size, màu, SKU, ...)
 *  - Log chi tiết kết quả từng bước
 * ============================================================
 */

(async function BulkAddProducts() {

  // ── Cấu hình ────────────────────────────────────────────
  const CONFIG = {
    // Base URL của backend. Thay đổi nếu backend chạy port khác.
    API_BASE: "https://webthoitrang.haorrr.dev/api",

    // Delay (ms) giữa các request để tránh quá tải server
    DELAY_MS: 300,
  };

  // ── Màu log ─────────────────────────────────────────────
  const log = {
    info: (m) => console.log(`%c  ${m}`, "color: #4FC3F7"),
    success: (m) => console.log(`%c✅ ${m}`, "color: #81C784; font-weight: bold"),
    warn: (m) => console.warn(`%c⚠️  ${m}`, "color: #FFB74D"),
    error: (m) => console.error(`%c❌ ${m}`, "color: #E57373; font-weight: bold"),
    title: (m) => console.log(`%c\n${"─".repeat(50)}\n${m}\n${"─".repeat(50)}`,
      "color: #CE93D8; font-size: 13px; font-weight: bold"),
    table: (data) => console.table(data),
  };

  // ── Lấy JWT token ────────────────────────────────────────
  function getToken() {
    // Thứ tự ưu tiên: accessToken → token → authToken
    const keys = ["accessToken", "token", "authToken", "jwt", "AUTH_TOKEN"];
    for (const key of keys) {
      const val = localStorage.getItem(key);
      if (val) return val;
    }
    // Thử tìm trong sessionStorage
    for (const key of keys) {
      const val = sessionStorage.getItem(key);
      if (val) return val;
    }
    return null;
  }

  // ── Gọi API helper ───────────────────────────────────────
  async function api(method, path, body = null, params = null) {
    const token = getToken();
    if (!token) throw new Error("Không tìm thấy JWT token trong localStorage/sessionStorage");

    let url = `${CONFIG.API_BASE}${path}`;
    if (params) {
      const qs = new URLSearchParams(params).toString();
      url = `${url}?${qs}`;
    }

    const res = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
      },
      body: body ? JSON.stringify(body) : null,
    });

    const json = await res.json();
    if (!res.ok || !json.success) {
      const msg = json.message || json.errors || res.statusText;
      throw new Error(`${res.status} — ${typeof msg === "object" ? JSON.stringify(msg) : msg}`);
    }
    return json.data;
  }

  // ── Delay helper ─────────────────────────────────────────
  const sleep = (ms) => new Promise(r => setTimeout(r, ms));

  // ── Kiểm tra dữ liệu đầu vào ─────────────────────────────
  if (!window.PRODUCTS_DATA || !Array.isArray(window.PRODUCTS_DATA) || window.PRODUCTS_DATA.length === 0) {
    log.error("Chưa có dữ liệu! Hãy paste products-data.js trước.");
    log.warn("window.PRODUCTS_DATA phải là một array sản phẩm.");
    return;
  }

  // ── Kiểm tra token ───────────────────────────────────────
  const token = getToken();
  if (!token) {
    log.error("Không tìm thấy JWT token!");
    log.warn("Kiểm tra localStorage: localStorage.getItem('accessToken')");
    log.warn("Nếu token lưu ở key khác, sửa hàm getToken() trong script.");
    return;
  }
  log.success(`Tìm thấy JWT token (${token.substring(0, 30)}...)`);

  // ── Kết quả tổng hợp ─────────────────────────────────────
  const results = {
    total: window.PRODUCTS_DATA.length,
    success: 0,
    failed: 0,
    details: [],
  };

  log.title(`BẮT ĐẦU THÊM ${results.total} SẢN PHẨM`);

  // ── Xử lý từng sản phẩm ──────────────────────────────────
  for (let i = 0; i < window.PRODUCTS_DATA.length; i++) {
    const p = window.PRODUCTS_DATA[i];
    const label = `[${i + 1}/${results.total}] "${p.name}"`;

    console.groupCollapsed(`%c${label}`, "color: #CE93D8; font-weight: bold");

    try {
      // STEP 1: Tạo sản phẩm
      log.info("Bước 1: Tạo sản phẩm...");
      const created = await api("POST", "/products", {
        name: p.name,
        description: p.description || null,
        price: p.price,
        salePrice: p.salePrice || null,
        stock: p.stock ?? 0,
        categoryId: p.categoryId || null,
      });
      const productId = created.id;
      log.success(`Sản phẩm tạo thành công — ID: ${productId}`);
      await sleep(CONFIG.DELAY_MS);

      // STEP 2: Thêm ảnh theo URL
      const images = p.images || [];
      if (images.length > 0) {
        log.info(`Bước 2: Thêm ${images.length} ảnh...`);
        for (let j = 0; j < images.length; j++) {
          const img = images[j];
          try {
            await api("POST", `/products/${productId}/images/url`, null, {
              imageUrl: img.url,
              isPrimary: img.isPrimary ? "true" : "false",
            });
            log.success(`  Ảnh ${j + 1}/${images.length}: ${img.isPrimary ? "[Primary] " : ""}${img.url.substring(0, 60)}...`);
          } catch (e) {
            log.warn(`  Ảnh ${j + 1} thất bại: ${e.message}`);
          }
          await sleep(CONFIG.DELAY_MS);
        }
      } else {
        log.info("Bước 2: Không có ảnh — bỏ qua.");
      }

      // STEP 3: Thêm biến thể
      const variants = p.variants || [];
      if (variants.length > 0) {
        log.info(`Bước 3: Thêm ${variants.length} biến thể...`);
        for (let k = 0; k < variants.length; k++) {
          const v = variants[k];
          try {
            await api("POST", `/products/${productId}/variants`, {
              size: v.size || null,
              color: v.color || null,
              colorCode: v.colorCode || null,
              sku: v.sku || null,
              stock: v.stock ?? 0,
              priceAdjustment: v.priceAdjustment ?? 0,
            });
            const vLabel = [v.size, v.color, v.sku].filter(Boolean).join(" / ");
            log.success(`  Biến thể ${k + 1}/${variants.length}: ${vLabel}`);
          } catch (e) {
            log.warn(`  Biến thể ${k + 1} thất bại: ${e.message}`);
          }
          await sleep(CONFIG.DELAY_MS);
        }
      } else {
        log.info("Bước 3: Không có biến thể — bỏ qua.");
      }

      results.success++;
      results.details.push({ stt: i + 1, name: p.name, id: productId, status: "✅ OK" });

    } catch (err) {
      results.failed++;
      results.details.push({ stt: i + 1, name: p.name, id: null, status: `❌ ${err.message}` });
      log.error(`Thất bại: ${err.message}`);
    }

    console.groupEnd();
    await sleep(CONFIG.DELAY_MS);
  }

  // ── Kết quả cuối ─────────────────────────────────────────
  log.title("KẾT QUẢ TỔNG HỢP");
  log.table(results.details);
  console.log("");

  if (results.failed === 0) {
    log.success(`Hoàn thành! Đã thêm thành công ${results.success}/${results.total} sản phẩm.`);
  } else {
    log.warn(`Hoàn thành với ${results.failed} lỗi. Thành công: ${results.success}/${results.total}.`);
    log.warn("Kiểm tra bảng trên để xem chi tiết lỗi.");
  }

  // Lưu kết quả vào window để tra cứu sau
  window.BULK_ADD_RESULTS = results;
  log.info("Kết quả lưu tại: window.BULK_ADD_RESULTS");

})();
