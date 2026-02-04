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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        CategoryJpaEntity category = new CategoryJpaEntity();
        given(categoryRepository.findAllByIds(cmd.categoryId())).willReturn(List.of(category));
        given(productRepository.save(any(ProductJpaEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ProductDetail result = productService.createProduct(cmd);

        // then
        assertThat(result.product().name()).isEqualTo(cmd.productName());
        verify(productRepository).save(any(ProductJpaEntity.class));
    }

    @Test
    @DisplayName("상품 조회 성공")
    void getProduct_success() {
        // given
        Long productId = 1L;
        ProductJpaEntity product = ProductJpaEntity.createProduct("상품A", new BigDecimal("1000"), 10, List.of(new CategoryJpaEntity()));
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
        ProductJpaEntity product = ProductJpaEntity.createProduct("상품A", new BigDecimal("1000"), 10, List.of(new CategoryJpaEntity()));
        given(productRepository.findById(cmd.productId())).willReturn(Optional.of(product));
        given(productRepository.save(any(ProductJpaEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ProductDetail result = productService.updateProduct(cmd);

        // then
        assertThat(result.product().name()).isEqualTo(cmd.name());
        assertThat(product.getPrice()).isEqualTo(cmd.price());
        assertThat(product.getStockQuantity()).isEqualTo(cmd.stockQuantity());
    }
}