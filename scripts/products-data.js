/**
 * ============================================================
 *  DANH SÁCH SẢN PHẨM CẦN THÊM
 *  Chỉnh sửa file này rồi copy toàn bộ vào F12 console
 *  trước khi chạy bulk-add-products.js
 * ============================================================
 *
 *  Cấu trúc mỗi sản phẩm:
 *  {
 *    name        : "Tên sản phẩm"         [bắt buộc]
 *    description : "Mô tả"                [tùy chọn]
 *    price       : 299000                 [bắt buộc, số nguyên VNĐ]
 *    salePrice   : 249000                 [tùy chọn, giá sale]
 *    stock       : 100                    [bắt buộc, tồn kho tổng]
 *    categoryId  : 1                      [tùy chọn, ID danh mục]
 *
 *    images: [                            [tùy chọn]
 *      { url: "https://...", isPrimary: true  },
 *      { url: "https://...", isPrimary: false },
 *    ]
 *
 *    variants: [                          [tùy chọn]
 *      {
 *        size           : "S"             [tùy chọn: S, M, L, XL, XXL, ...]
 *        color          : "Đỏ"           [tùy chọn]
 *        colorCode      : "#FF0000"       [tùy chọn, mã hex]
 *        sku            : "PROD-S-RED"    [tùy chọn, mã SKU]
 *        stock          : 30             [mặc định 0]
 *        priceAdjustment: 0              [mặc định 0, cộng/trừ so với giá gốc]
 *      }
 *    ]
 *  }
 */

window.PRODUCTS_DATA = [

  // ── Sản phẩm 1 ──────────────────────────────────────────
  {
    name: "Áo thun basic unisex",
    description: "Áo thun cotton 100%, form rộng thoải mái, phù hợp mọi dáng người.",
    price: 199000,
    salePrice: 159000,
    stock: 200,
    categoryId: 1,
    images: [
      { url: "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800", isPrimary: true },
      { url: "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=800", isPrimary: false },
    ],
    variants: [
      { size: "S",  color: "Trắng", colorCode: "#FFFFFF", sku: "ATBASIC-S-WHITE",  stock: 40, priceAdjustment: 0 },
      { size: "M",  color: "Trắng", colorCode: "#FFFFFF", sku: "ATBASIC-M-WHITE",  stock: 60, priceAdjustment: 0 },
      { size: "L",  color: "Trắng", colorCode: "#FFFFFF", sku: "ATBASIC-L-WHITE",  stock: 50, priceAdjustment: 0 },
      { size: "XL", color: "Trắng", colorCode: "#FFFFFF", sku: "ATBASIC-XL-WHITE", stock: 50, priceAdjustment: 10000 },
      { size: "S",  color: "Đen",   colorCode: "#000000", sku: "ATBASIC-S-BLACK",  stock: 40, priceAdjustment: 0 },
      { size: "M",  color: "Đen",   colorCode: "#000000", sku: "ATBASIC-M-BLACK",  stock: 60, priceAdjustment: 0 },
      { size: "L",  color: "Đen",   colorCode: "#000000", sku: "ATBASIC-L-BLACK",  stock: 50, priceAdjustment: 0 },
      { size: "XL", color: "Đen",   colorCode: "#000000", sku: "ATBASIC-XL-BLACK", stock: 50, priceAdjustment: 10000 },
    ],
  },

  // ── Sản phẩm 2 ──────────────────────────────────────────
  {
    name: "Quần jeans slim fit nam",
    description: "Quần jeans denim cao cấp, form slim fit, wash nhẹ thời thượng.",
    price: 450000,
    salePrice: null,
    stock: 150,
    categoryId: 2,
    images: [
      { url: "https://images.unsplash.com/photo-1542272604-787c3835535d?w=800", isPrimary: true },
      { url: "https://images.unsplash.com/photo-1555689502-c4b22d76c56f?w=800", isPrimary: false },
    ],
    variants: [
      { size: "28", color: "Xanh đậm", colorCode: "#1E3A5F", sku: "JEANS-28-DARK",  stock: 25, priceAdjustment: 0 },
      { size: "29", color: "Xanh đậm", colorCode: "#1E3A5F", sku: "JEANS-29-DARK",  stock: 30, priceAdjustment: 0 },
      { size: "30", color: "Xanh đậm", colorCode: "#1E3A5F", sku: "JEANS-30-DARK",  stock: 35, priceAdjustment: 0 },
      { size: "31", color: "Xanh đậm", colorCode: "#1E3A5F", sku: "JEANS-31-DARK",  stock: 30, priceAdjustment: 0 },
      { size: "32", color: "Xanh đậm", colorCode: "#1E3A5F", sku: "JEANS-32-DARK",  stock: 30, priceAdjustment: 20000 },
      { size: "30", color: "Xanh nhạt", colorCode: "#7BAFD4", sku: "JEANS-30-LIGHT", stock: 30, priceAdjustment: 0 },
      { size: "31", color: "Xanh nhạt", colorCode: "#7BAFD4", sku: "JEANS-31-LIGHT", stock: 25, priceAdjustment: 0 },
      { size: "32", color: "Xanh nhạt", colorCode: "#7BAFD4", sku: "JEANS-32-LIGHT", stock: 25, priceAdjustment: 20000 },
    ],
  },

  // ── Sản phẩm 3 ──────────────────────────────────────────
  {
    name: "Váy midi floral nữ",
    description: "Váy dài qua gối họa tiết hoa, chất liệu voan mềm mại, phù hợp đi chơi hoặc dạo phố.",
    price: 350000,
    salePrice: 299000,
    stock: 80,
    categoryId: 3,
    images: [
      { url: "https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=800", isPrimary: true },
      { url: "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=800", isPrimary: false },
    ],
    variants: [
      { size: "S",  color: "Hồng hoa", colorCode: "#FFB6C1", sku: "VAYFLORAL-S-PINK",   stock: 20, priceAdjustment: 0 },
      { size: "M",  color: "Hồng hoa", colorCode: "#FFB6C1", sku: "VAYFLORAL-M-PINK",   stock: 25, priceAdjustment: 0 },
      { size: "L",  color: "Hồng hoa", colorCode: "#FFB6C1", sku: "VAYFLORAL-L-PINK",   stock: 20, priceAdjustment: 0 },
      { size: "S",  color: "Xanh hoa", colorCode: "#87CEEB", sku: "VAYFLORAL-S-BLUE",   stock: 15, priceAdjustment: 0 },
      { size: "M",  color: "Xanh hoa", colorCode: "#87CEEB", sku: "VAYFLORAL-M-BLUE",   stock: 15, priceAdjustment: 0 },
      { size: "L",  color: "Xanh hoa", colorCode: "#87CEEB", sku: "VAYFLORAL-L-BLUE",   stock: 15, priceAdjustment: 0 },
    ],
  },

  // ── Thêm sản phẩm mới ở đây ────────────────────────────
  // {
  //   name: "...",
  //   price: 0,
  //   stock: 0,
  //   categoryId: null,
  //   images: [],
  //   variants: [],
  // },

];

console.log(`%c✅ Đã load ${window.PRODUCTS_DATA.length} sản phẩm vào bộ nhớ`, "color: green; font-weight: bold");
console.log("Chạy tiếp bulk-add-products.js để bắt đầu thêm...");
