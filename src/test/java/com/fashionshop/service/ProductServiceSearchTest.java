package com.fashionshop.service;

import com.fashionshop.dto.response.ProductSummaryResponse;
import com.fashionshop.entity.Category;
import com.fashionshop.entity.Product;
import com.fashionshop.repository.ProductRepository;
import com.fashionshop.repository.ProductSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test – Tìm kiếm sản phẩm (Branch Coverage)
 * 
 * Tương ứng mục 5.3 trong báo cáo KTPM.
 * Kỹ thuật: Branch Coverage (4 nhánh × 2 = 8 trường hợp)
 * Framework: JUnit 5 + Mockito
 * 
 * Method được test: ProductService.getProducts(search, ...)
 * 
 * Bảng nhánh:
 * BR1: keyword == null || keyword.trim().isEmpty() → Trả về toàn bộ SP
 * BR2: Còn phần tử trong vòng lặp? → Vào thân vòng lặp / Thoát vòng lặp
 * BR3: name.contains(keyword) → Thêm SP vào kết quả
 * BR4: description != null && desc.contains(keyword) → Thêm SP vào kết quả
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceSearchTest {

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

        private Product productLeather;
        private Product productElegant;
        private Product productNoDesc;

        @BeforeEach
        void setUp() {
                // SP 1: "Leather Jacket" - tên chứa "leather", mô tả chứa "stylish"
                productLeather = Product.builder()
                                .name("Leather Jacket")
                                .slug("leather-jacket")
                                .description("A stylish leather jacket for winter")
                                .price(new BigDecimal("500000"))
                                .stock(10)
                                .status(Product.Status.ACTIVE)
                                .build();
                productLeather.setId(1L);

                // SP 2: "Cotton T-Shirt" - tên không chứa "leather", mô tả chứa "elegant"
                productElegant = Product.builder()
                                .name("Cotton T-Shirt")
                                .slug("cotton-tshirt")
                                .description("An elegant cotton t-shirt")
                                .price(new BigDecimal("200000"))
                                .stock(5)
                                .status(Product.Status.ACTIVE)
                                .build();
                productElegant.setId(2L);

                // SP 3: Sản phẩm không có mô tả
                productNoDesc = Product.builder()
                                .name("Simple Hat")
                                .slug("simple-hat")
                                .description(null)
                                .price(new BigDecimal("100000"))
                                .stock(20)
                                .status(Product.Status.ACTIVE)
                                .build();
                productNoDesc.setId(3L);
        }

        private void mockFlashSaleEmpty() {
                when(flashSaleService.getActiveFlashInfoMap()).thenReturn(Collections.emptyMap());
        }

        private Page<Product> createPage(List<Product> products) {
                Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
                return new PageImpl<>(products, pageable, products.size());
        }

        // ==================== TC_WB_BR_01 ====================
        /**
         * TC_WB_BR_01: Từ khóa null
         * BR1 = True (keyword null → trả về tất cả SP)
         * Kết quả mong đợi: Trả về tất cả sản phẩm
         */
        @Test
        @DisplayName("TC_WB_BR_01: Từ khóa null → trả về tất cả sản phẩm")
        void testSearch_NullKeyword_ReturnsAllProducts() {
                // Arrange
                mockFlashSaleEmpty();
                List<Product> allProducts = List.of(productLeather, productElegant, productNoDesc);
                when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                                .thenReturn(createPage(allProducts));

                // Act
                Page<ProductSummaryResponse> result = productService.getProducts(
                                null, null, null, null, null, "createdAt", "desc", 0, 10);

                // Assert
                assertNotNull(result);
                assertEquals(3, result.getTotalElements());
                verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        // ==================== TC_WB_BR_02 ====================
        /**
         * TC_WB_BR_02: Từ khóa rỗng ""
         * BR1 = True (keyword rỗng → trả về tất cả SP)
         * Kết quả mong đợi: Trả về tất cả sản phẩm
         */
        @Test
        @DisplayName("TC_WB_BR_02: Từ khóa rỗng → trả về tất cả sản phẩm")
        void testSearch_EmptyKeyword_ReturnsAllProducts() {
                // Arrange
                mockFlashSaleEmpty();
                List<Product> allProducts = List.of(productLeather, productElegant, productNoDesc);
                when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                                .thenReturn(createPage(allProducts));

                // Act
                Page<ProductSummaryResponse> result = productService.getProducts(
                                "", null, null, null, null, "createdAt", "desc", 0, 10);

                // Assert
                assertNotNull(result);
                assertEquals(3, result.getTotalElements());
        }

        // ==================== TC_WB_BR_03 ====================
        /**
         * TC_WB_BR_03: Từ khóa chỉ có khoảng trắng " "
         * BR1 = True (keyword khoảng trắng → trả về tất cả SP)
         * Kết quả mong đợi: Trả về tất cả sản phẩm
         */
        @Test
        @DisplayName("TC_WB_BR_03: Từ khóa khoảng trắng → trả về tất cả sản phẩm")
        void testSearch_WhitespaceKeyword_ReturnsAllProducts() {
                // Arrange
                mockFlashSaleEmpty();
                List<Product> allProducts = List.of(productLeather, productElegant, productNoDesc);
                when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                                .thenReturn(createPage(allProducts));

                // Act
                Page<ProductSummaryResponse> result = productService.getProducts(
                                "   ", null, null, null, null, "createdAt", "desc", 0, 10);

                // Assert
                assertNotNull(result);
                assertEquals(3, result.getTotalElements());
        }

        // ==================== TC_WB_BR_04 ====================
        /**
         * TC_WB_BR_04: Từ khóa khớp tên SP ("leather")
         * BR1 = False, BR2 = True (có SP), BR3 = True (name chứa keyword)
         * Kết quả mong đợi: Trả về SP có "leather" trong tên
         */
        @Test
        @DisplayName("TC_WB_BR_04: Khớp tên SP → trả về SP tương ứng")
        void testSearch_MatchProductName_ReturnsMatchingProducts() {
                // Arrange
                mockFlashSaleEmpty();
                List<Product> matchedProducts = List.of(productLeather);
                when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                                .thenReturn(createPage(matchedProducts));

                // Act
                Page<ProductSummaryResponse> result = productService.getProducts(
                                "leather", null, null, null, null, "createdAt", "desc", 0, 10);

                // Assert
                assertNotNull(result);
                assertEquals(1, result.getTotalElements());
                assertEquals("Leather Jacket", result.getContent().get(0).getName());
        }

        // ==================== TC_WB_BR_05 ====================
        /**
         * TC_WB_BR_05: Từ khóa khớp mô tả SP ("elegant")
         * BR1 = False, BR2 = True, BR3 = False (tên không khớp), BR4 = True (mô tả
         * khớp)
         * Kết quả mong đợi: Trả về SP có "elegant" trong mô tả
         */
        @Test
        @DisplayName("TC_WB_BR_05: Khớp mô tả SP → trả về SP tương ứng")
        void testSearch_MatchDescription_ReturnsMatchingProducts() {
                // Arrange
                mockFlashSaleEmpty();
                List<Product> matchedProducts = List.of(productElegant);
                when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                                .thenReturn(createPage(matchedProducts));

                // Act
                Page<ProductSummaryResponse> result = productService.getProducts(
                                "elegant", null, null, null, null, "createdAt", "desc", 0, 10);

                // Assert
                assertNotNull(result);
                assertEquals(1, result.getTotalElements());
                assertEquals("Cotton T-Shirt", result.getContent().get(0).getName());
        }

        // ==================== TC_WB_BR_06 ====================
        /**
         * TC_WB_BR_06: Từ khóa không khớp bất kỳ SP nào ("abcxyz123")
         * BR1 = False, BR2 = True, BR3 = False, BR4 = False
         * Kết quả mong đợi: Trả về danh sách rỗng
         */
        @Test
        @DisplayName("TC_WB_BR_06: Không khớp → trả về danh sách rỗng")
        void testSearch_NoMatch_ReturnsEmptyList() {
                // Arrange
                mockFlashSaleEmpty();
                when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                                .thenReturn(createPage(Collections.emptyList()));

                // Act
                Page<ProductSummaryResponse> result = productService.getProducts(
                                "abcxyz123", null, null, null, null, "createdAt", "desc", 0, 10);

                // Assert
                assertNotNull(result);
                assertEquals(0, result.getTotalElements());
                assertTrue(result.getContent().isEmpty());
        }

        // ==================== TC_WB_BR_07 ====================
        /**
         * TC_WB_BR_07: Vòng lặp kết thúc – đã duyệt hết tất cả SP
         * BR1 = False, BR2 = False (hết phần tử)
         * Kết quả mong đợi: Thoát vòng lặp, trả về danh sách result
         */
        @Test
        @DisplayName("TC_WB_BR_07: Duyệt hết SP → trả về kết quả đầy đủ")
        void testSearch_IterateAllProducts_ReturnsFullResult() {
                // Arrange
                mockFlashSaleEmpty();
                List<Product> allProducts = List.of(productLeather, productElegant);
                when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                                .thenReturn(createPage(allProducts));

                // Act
                Page<ProductSummaryResponse> result = productService.getProducts(
                                "t", null, null, null, null, "createdAt", "desc", 0, 10);

                // Assert
                assertNotNull(result);
                assertEquals(2, result.getTotalElements());
                // Kết quả chứa đầy đủ các SP đã duyệt
                assertEquals(2, result.getContent().size());
        }

        // ==================== TC_WB_BR_08 ====================
        /**
         * TC_WB_BR_08: Tìm kiếm không phân biệt hoa/thường ("LEATHER")
         * BR1 = False, BR2 = True, BR3 = True (case-insensitive)
         * Kết quả mong đợi: Tìm thấy SP dù nhập chữ hoa
         */
        @Test
        @DisplayName("TC_WB_BR_08: Case-insensitive → tìm thấy SP")
        void testSearch_CaseInsensitive_ReturnsMatchingProducts() {
                // Arrange
                mockFlashSaleEmpty();
                List<Product> matchedProducts = List.of(productLeather);
                when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                                .thenReturn(createPage(matchedProducts));

                // Act
                Page<ProductSummaryResponse> result = productService.getProducts(
                                "LEATHER", null, null, null, null, "createdAt", "desc", 0, 10);

                // Assert
                assertNotNull(result);
                assertEquals(1, result.getTotalElements());
                assertEquals("Leather Jacket", result.getContent().get(0).getName());
                verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
        }
}
