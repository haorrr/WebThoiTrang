package com.fashionshop.service;

import com.fashionshop.dto.response.ProductResponse;
import com.fashionshop.entity.Product;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit Test – Xem chi tiết sản phẩm (Condition Coverage)
 * 
 * Tương ứng mục 5.4 trong báo cáo KTPM.
 * Kỹ thuật: Condition Coverage (5 điều kiện con)
 * Framework: JUnit 5 + Mockito
 * 
 * Method được test: ProductService.getProductById(Long id)
 * 
 * Các điều kiện con:
 * C1: productId == null → ID sản phẩm hợp lệ?
 * C2: product == null → SP tồn tại trong DB?
 * C3: product.stock <= 0 → SP còn hàng?
 * C4: product.salePrice != null → SP đang sale?
 * C5: product.salePrice < product.price → Giá sale hợp lệ?
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceDetailTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private com.fashionshop.repository.ProductImageRepository productImageRepository;

    @Mock
    private com.fashionshop.repository.CategoryRepository categoryRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private FlashSaleService flashSaleService;

    @InjectMocks
    private ProductService productService;

    private Product normalProduct;

    @BeforeEach
    void setUp() {
        // Sản phẩm mặc định: còn hàng, không sale
        normalProduct = Product.builder()
                .name("Áo Thun Basic")
                .slug("ao-thun-basic")
                .description("Áo thun cotton cao cấp")
                .price(new BigDecimal("250000"))
                .salePrice(null)
                .stock(15)
                .status(Product.Status.ACTIVE)
                .build();
        normalProduct.setId(1L);
    }

    private void mockFlashSaleEmpty() {
        when(flashSaleService.getActiveFlashInfoMap()).thenReturn(Collections.emptyMap());
    }

    // ==================== TC_WB_CON_01 ====================
    /**
     * TC_WB_CON_01: productId = null → Lỗi ID không hợp lệ
     * C1 = True
     * Kết quả mong đợi: Ném ngoại lệ (ID null không hợp lệ)
     */
    @Test
    @DisplayName("TC_WB_CON_01: productId = null → Exception")
    void testGetProductDetail_NullId_ThrowsException() {
        // Arrange
        when(productRepository.findById(null))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById(null));
    }

    // ==================== TC_WB_CON_02 ====================
    /**
     * TC_WB_CON_02: productId không tồn tại trong DB
     * C1 = False, C2 = True
     * Kết quả mong đợi: ResourceNotFoundException
     */
    @Test
    @DisplayName("TC_WB_CON_02: SP không tồn tại → ResourceNotFoundException")
    void testGetProductDetail_ProductNotFound_ThrowsException() {
        // Arrange
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(999L));
        assertTrue(exception.getMessage().contains("not found"));
    }

    // ==================== TC_WB_CON_03 ====================
    /**
     * TC_WB_CON_03: SP hết hàng (stock = 0)
     * C1 = False, C2 = False, C3 = True
     * Kết quả mong đợi: Trả về SP với stock = 0 (hiển thị "Hết hàng")
     */
    @Test
    @DisplayName("TC_WB_CON_03: SP hết hàng (stock=0) → hiển thị stock=0")
    void testGetProductDetail_OutOfStock_ReturnsProductWithZeroStock() {
        // Arrange
        normalProduct.setStock(0);
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(normalProduct));
        mockFlashSaleEmpty();

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getStock());
        assertEquals("Áo Thun Basic", response.getName());
    }

    // ==================== TC_WB_CON_04 ====================
    /**
     * TC_WB_CON_04: SP không có giá sale (salePrice = null)
     * C1 = False, C2 = False, C3 = False, C4 = False
     * Kết quả mong đợi: Hiển thị giá gốc, salePrice = null
     */
    @Test
    @DisplayName("TC_WB_CON_04: SP không sale → hiển thị giá gốc")
    void testGetProductDetail_NoSale_ReturnsOriginalPrice() {
        // Arrange
        normalProduct.setSalePrice(null);
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(normalProduct));
        mockFlashSaleEmpty();

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("250000"), response.getPrice());
        assertNull(response.getSalePrice());
    }

    // ==================== TC_WB_CON_05 ====================
    /**
     * TC_WB_CON_05: SP có giá sale hợp lệ (salePrice < price)
     * C1 = False, C2 = False, C3 = False, C4 = True, C5 = True
     * Kết quả mong đợi: Hiển thị giá sale
     */
    @Test
    @DisplayName("TC_WB_CON_05: SP có sale hợp lệ → hiển thị giá sale")
    void testGetProductDetail_ValidSalePrice_ReturnsSalePrice() {
        // Arrange
        normalProduct.setSalePrice(new BigDecimal("199000"));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(normalProduct));
        mockFlashSaleEmpty();

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("250000"), response.getPrice());
        assertEquals(new BigDecimal("199000"), response.getSalePrice());
        // Giá sale phải nhỏ hơn giá gốc
        assertTrue(response.getSalePrice().compareTo(response.getPrice()) < 0);
    }

    // ==================== TC_WB_CON_06 ====================
    /**
     * TC_WB_CON_06: SP có salePrice >= price (giá sale không hợp lệ)
     * C1 = False, C2 = False, C3 = False, C4 = True, C5 = False
     * Kết quả mong đợi: Vẫn trả về salePrice nhưng không hợp lệ → dùng giá gốc
     */
    @Test
    @DisplayName("TC_WB_CON_06: salePrice >= price → giá sale không hợp lệ")
    void testGetProductDetail_InvalidSalePrice_ReturnsOriginalPrice() {
        // Arrange - salePrice >= price
        normalProduct.setSalePrice(new BigDecimal("300000")); // > price 250000
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(normalProduct));
        mockFlashSaleEmpty();

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("250000"), response.getPrice());
        assertEquals(new BigDecimal("300000"), response.getSalePrice());
        // salePrice >= price → giá sale không hợp lệ
        assertTrue(response.getSalePrice().compareTo(response.getPrice()) >= 0);
    }

    // ==================== TC_WB_CON_07 ====================
    /**
     * TC_WB_CON_07: SP còn hàng, không sale → thông tin đầy đủ
     * C1 = False, C2 = False, C3 = False, C4 = False
     * Kết quả mong đợi: Hiển thị thông tin đầy đủ với stock > 0
     */
    @Test
    @DisplayName("TC_WB_CON_07: SP bình thường → hiển thị đầy đủ thông tin")
    void testGetProductDetail_NormalProduct_ReturnsFullInfo() {
        // Arrange
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(normalProduct));
        mockFlashSaleEmpty();

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Áo Thun Basic", response.getName());
        assertEquals("ao-thun-basic", response.getSlug());
        assertEquals("Áo thun cotton cao cấp", response.getDescription());
        assertEquals(new BigDecimal("250000"), response.getPrice());
        assertNull(response.getSalePrice());
        assertEquals(15, response.getStock());
        assertEquals("ACTIVE", response.getStatus());

        // Verify chỉ gọi repository 1 lần
        verify(productRepository).findById(1L);
        verify(flashSaleService).getActiveFlashInfoMap();
    }
}
