package kcp.product.service;

import kcp.common.exception.BusinessException;
import kcp.common.exception.ErrorCode;
import kcp.product.service.dto.ProductCreateCmd;
import kcp.product.service.dto.ProductDetail;
import kcp.product.service.dto.ProductUpdateCmd;
import kcp.product.service.entity.CategoryJpaEntity;
import kcp.product.service.entity.ProductJpaEntity;
import kcp.product.service.repository.CategoryRepository;
import kcp.product.service.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("상품 생성 성공")
    void createProduct_success() {
        // given
        ProductCreateCmd cmd = new ProductCreateCmd(List.of(1L), "상품A", new BigDecimal("1000"), 10);
        CategoryJpaEntity category = CategoryJpaEntity.createTestEmptyCategory();
        given(categoryRepository.findAllByIds(cmd.categoryId())).willReturn(List.of(category));
        given(productRepository.save(any(ProductJpaEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ProductDetail result = productService.createProduct(cmd);

        // then
        assertThat(result.product().name()).isEqualTo(cmd.productName());
        verify(productRepository).save(any(ProductJpaEntity.class));
    }

    @Test
    @DisplayName("카테고리 없이 상품 생성 시 예외 발생(CATEGORY_REQUIRED)")
    void createProduct_noCategory_throwsException() {
        // given
        ProductCreateCmd cmd = new ProductCreateCmd(List.of(1L), "상품A", new BigDecimal("1000"), 10);
        given(categoryRepository.findAllByIds(cmd.categoryId())).willReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> productService.createProduct(cmd))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_REQUIRED);
    }

    @Test
    @DisplayName("상품 조회 성공")
    void getProduct_success() {
        // given
        Long productId = 1L;
        ProductJpaEntity product = ProductJpaEntity.createProduct("상품A", new BigDecimal("1000"), 10, List.of(CategoryJpaEntity.createTestEmptyCategory()));
        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        ProductDetail result = productService.getProduct(productId);

        // then
        assertThat(result.product().name()).isEqualTo(product.getName());
    }

    @Test
    @DisplayName("상품 조회 실패 시 예외 발생")
    void getProduct_notFound_throwsException() {
        // given
        Long productId = 1L;
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(productId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateProduct_success() {
        // given
        ProductUpdateCmd cmd = new ProductUpdateCmd(1L, "수정상품", new BigDecimal("2000"), 20, List.of(1L));
        ProductJpaEntity product = ProductJpaEntity.createProduct("상품A", new BigDecimal("1000"), 10, List.of(CategoryJpaEntity.createTestEmptyCategory()));
        given(productRepository.findById(cmd.productId())).willReturn(Optional.of(product));
        given(categoryRepository.findAllByIds(anyList())).willReturn(List.of(CategoryJpaEntity.createTestEmptyCategory()));
        given(productRepository.save(any(ProductJpaEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ProductDetail result = productService.updateProduct(cmd);

        // then
        assertThat(result.product().name()).isEqualTo(cmd.name());
        assertThat(product.getPrice()).isEqualTo(cmd.price());
        assertThat(product.getStockQuantity()).isEqualTo(cmd.stockQuantity());
    }

    @Test
    @DisplayName("상품 수정 실패 - 대상 상품이 존재하지 않으면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void updateProduct_notFound_throwsException() {
        // given
        Long invalidProductId = 999L;
        ProductUpdateCmd cmd = new ProductUpdateCmd(invalidProductId, "수정상품", new BigDecimal("2000"), 20, List.of(1L));

        given(productRepository.findById(invalidProductId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.updateProduct(cmd))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 수정 실패 - 일부 카테고리 ID가 존재하지 않으면 CATEGORY_NOT_FOUND 예외와 누락된 ID 목록을 반환한다")
    void updateProduct_categoryNotFound_throwsException() {
        // given
        Long productId = 1L;
        List<Long> requestedCategoryIds = List.of(10L, 20L, 30L);
        ProductUpdateCmd cmd = new ProductUpdateCmd(productId, "수정상품", new BigDecimal("2000"), 20, requestedCategoryIds);

        // 상품은 존재함
        ProductJpaEntity product = ProductJpaEntity.createProduct("상품A", new BigDecimal("1000"), 10, List.of(CategoryJpaEntity.createTestEmptyCategory()));

        ReflectionTestUtils.setField(product, "id", productId);
        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // 카테고리는 10L 하나만 존재 (20L, 30L은 누락됨)
        CategoryJpaEntity category10 = CategoryJpaEntity.createTestEmptyCategory();
        ReflectionTestUtils.setField(category10, "id", 10L);

        given(categoryRepository.findAllByIds(anyList())).willReturn(List.of(category10));

        // when & then
        assertThatThrownBy(() -> productService.updateProduct(cmd))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
    }
}