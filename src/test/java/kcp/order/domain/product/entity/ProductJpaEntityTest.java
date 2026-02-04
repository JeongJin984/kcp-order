package kcp.order.domain.product.entity;

import kcp.order.common.exception.BusinessException;
import kcp.order.common.exception.ErrorCode;
import kcp.order.common.exception.OutOfStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductJpaEntityTest {

    @Test
    @DisplayName("상품 생성 성공")
    void createProduct_success() {
        // given
        String name = "상품A";
        BigDecimal price = new BigDecimal("1000");
        int stockQuantity = 10;
        CategoryJpaEntity category = new CategoryJpaEntity(); // 실제로는 name 필드가 필요할 수 있으나 엔티티 내부에서 쓰이지 않음

        // when
        ProductJpaEntity product = ProductJpaEntity.createProduct(name, price, stockQuantity, List.of(category));

        // then
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getPrice()).isEqualTo(price);
        assertThat(product.getStockQuantity()).isEqualTo(stockQuantity);
        assertThat(product.getProductCategories()).hasSize(1);
    }

    @Test
    @DisplayName("카테고리 없이 상품 생성 시 예외 발생")
    void createProduct_noCategory_throwsException() {
        // given
        String name = "상품A";
        BigDecimal price = new BigDecimal("1000");
        int stockQuantity = 10;

        // when & then
        assertThatThrownBy(() -> ProductJpaEntity.createProduct(name, price, stockQuantity, Collections.emptyList()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_REQUIRED);
    }

    @Test
    @DisplayName("재고 감소 성공")
    void decreaseStock_success() {
        // given
        ProductJpaEntity product = ProductJpaEntity.createProduct("상품A", new BigDecimal("1000"), 10, List.of(new CategoryJpaEntity()));

        // when
        product.decreaseStock(5);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생")
    void decreaseStock_outOfStock_throwsException() {
        // given
        ProductJpaEntity product = ProductJpaEntity.createProduct("상품A", new BigDecimal("1000"), 10, List.of(new CategoryJpaEntity()));

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(11))
                .isInstanceOf(OutOfStockException.class);
    }

    @Test
    @DisplayName("재고 증가 성공")
    void increaseStock_success() {
        // given
        ProductJpaEntity product = ProductJpaEntity.createProduct("상품A", new BigDecimal("1000"), 10, List.of(new CategoryJpaEntity()));

        // when
        product.increaseStock(5);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(15);
    }
}