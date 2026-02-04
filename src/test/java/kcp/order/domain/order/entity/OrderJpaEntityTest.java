package kcp.order.domain.order.entity;

import kcp.order.common.exception.InvalidOrderStatusException;
import kcp.order.domain.product.entity.CategoryJpaEntity;
import kcp.order.domain.product.entity.ProductJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderJpaEntityTest {

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_success() {
        // given
        ProductJpaEntity product = createProduct("상품A", 1000, 10);
        OrderItemJpaEntity orderItem = OrderItemJpaEntity.createOrderItem(product, new BigDecimal("1000"), 2);

        // when
        OrderJpaEntity order = OrderJpaEntity.createOrder(List.of(orderItem));

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.WAIT);
        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(order.getOrderItems().get(0).getOrder()).isEqualTo(order);
    }

    @Test
    @DisplayName("주문 수락 성공")
    void accept_success() {
        // given
        OrderJpaEntity order = OrderJpaEntity.createOrder(List.of());

        // when
        order.accept();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    @DisplayName("WAIT 상태가 아닐 때 수락 시 예외 발생")
    void accept_invalidStatus_throwsException() {
        // given
        OrderJpaEntity order = OrderJpaEntity.createOrder(List.of());
        order.accept(); // ACCEPTED 상태로 변경

        // when & then
        assertThatThrownBy(order::accept)
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("주문 완료 시 재고 차감")
    void complete_success() {
        // given
        ProductJpaEntity product = createProduct("상품A", 1000, 10);
        OrderItemJpaEntity orderItem = OrderItemJpaEntity.createOrderItem(product, new BigDecimal("1000"), 2);
        OrderJpaEntity order = OrderJpaEntity.createOrder(List.of(orderItem));
        order.accept();

        // when
        order.complete();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(product.getStockQuantity()).isEqualTo(8);
    }

    @Test
    @DisplayName("ACCEPTED 상태가 아닐 때 완료 시 예외 발생")
    void complete_invalidStatus_throwsException() {
        // given
        OrderJpaEntity order = OrderJpaEntity.createOrder(List.of());

        // when & then
        assertThatThrownBy(order::complete)
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("주문 취소 시 상태 변경")
    void cancel_wait_success() {
        // given
        OrderJpaEntity order = OrderJpaEntity.createOrder(List.of());

        // when
        order.cancel();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("완료된 주문 취소 시 재고 복구")
    void cancel_completedOrder_restoresStock() {
        // given
        ProductJpaEntity product = createProduct("상품A", 1000, 10);
        OrderItemJpaEntity orderItem = OrderItemJpaEntity.createOrderItem(product, new BigDecimal("1000"), 2);
        OrderJpaEntity order = OrderJpaEntity.createOrder(List.of(orderItem));
        order.accept();
        order.complete();
        assertThat(product.getStockQuantity()).isEqualTo(8);

        // when
        order.cancel();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    private ProductJpaEntity createProduct(String name, int price, int stock) {
        return ProductJpaEntity.createProduct(name, new BigDecimal(price), stock, List.of(new CategoryJpaEntity()));
    }
}