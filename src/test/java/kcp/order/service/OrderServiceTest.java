package kcp.order.service;

import kcp.common.exception.BusinessException;
import kcp.common.exception.ErrorCode;
import kcp.common.exception.InvalidOrderStatusException;
import kcp.order.service.dto.OrderCreateCmd;
import kcp.order.service.dto.OrderDetail;
import kcp.order.service.entity.OrderJpaEntity;
import kcp.order.service.entity.OrderStatus;
import kcp.order.service.repository.OrderRepository;
import kcp.product.service.entity.CategoryJpaEntity;
import kcp.product.service.entity.ProductJpaEntity;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 등록 성공")
    void registerOrder_success() {
        // given
        OrderCreateCmd cmd = new OrderCreateCmd(List.of(new OrderCreateCmd.OrderItem(1L, 2)));
        ProductJpaEntity product = createProduct("상품A", 1000, 10);
        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.of(product));
        given(orderRepository.save(any(OrderJpaEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        OrderDetail result = orderService.registerOrder(cmd);

        // then
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.WAIT);
        assertThat(result.items()).hasSize(1);
        verify(orderRepository).save(any(OrderJpaEntity.class));
    }

    @Test
    @DisplayName("주문 등록 시 재고는 차감되지 않는다(완료 시점에 차감)")
    void registerOrder_doesNotDecreaseStock() {
        // given
        ProductJpaEntity product = createProduct("상품A", 1000, 10);
        OrderCreateCmd cmd = new OrderCreateCmd(List.of(new OrderCreateCmd.OrderItem(1L, 2)));

        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.of(product));
        given(orderRepository.save(any(OrderJpaEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        orderService.registerOrder(cmd);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("여러 상품(다건 아이템) 주문 등록 성공")
    void registerOrder_multipleItems_success() {
        // given
        OrderCreateCmd cmd = new OrderCreateCmd(List.of(
            new OrderCreateCmd.OrderItem(1L, 2),
            new OrderCreateCmd.OrderItem(2L, 1)
        ));
        ProductJpaEntity product1 = createProduct("상품A", 1000, 10);
        ProductJpaEntity product2 = createProduct("상품B", 2000, 5);

        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.of(product1));
        given(productRepository.findByIdWithLock(2L)).willReturn(Optional.of(product2));
        given(orderRepository.save(any(OrderJpaEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        OrderDetail result = orderService.registerOrder(cmd);

        // then
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.WAIT);
        assertThat(result.items()).hasSize(2);
        verify(productRepository).findByIdWithLock(1L);
        verify(productRepository).findByIdWithLock(2L);
        verify(orderRepository).save(any(OrderJpaEntity.class));
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 주문 시 예외 발생")
    void registerOrder_productNotFound_throwsException() {
        // given
        OrderCreateCmd cmd = new OrderCreateCmd(List.of(new OrderCreateCmd.OrderItem(1L, 2)));
        given(productRepository.findByIdWithLock(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.registerOrder(cmd))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 상태 변경 성공 - 수락")
    void changeStatus_accept_success() {
        // given
        Long orderId = 1L;
        OrderJpaEntity order = OrderJpaEntity.createOrder(List.of());
        given(orderRepository.findByIdWithProductAndLock(orderId)).willReturn(Optional.of(order));

        // when
        OrderDetail result = orderService.changeStatus(orderId, OrderStatus.ACCEPTED);

        // then
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    @DisplayName("주문이 없으면 상태 변경 시 ORDER_NOT_FOUND 예외 발생")
    void changeStatus_orderNotFound_throwsException() {
        // given
        Long orderId = 999L;
        given(orderRepository.findByIdWithProductAndLock(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.changeStatus(orderId, OrderStatus.ACCEPTED))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 상태 변경 실패 - 잘못된 전이(예: WAIT -> COMPLETED)면 INVALID_ORDER_STATUS 예외 발생")
    void changeStatus_invalidTransition_throwsInvalidOrderStatusException() {
        // given
        Long orderId = 1L;
        OrderJpaEntity order = OrderJpaEntity.createOrder(List.of()); // WAIT
        given(orderRepository.findByIdWithProductAndLock(orderId)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.changeStatus(orderId, OrderStatus.COMPLETED))
            .isInstanceOf(InvalidOrderStatusException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    private ProductJpaEntity createProduct(String name, int price, int stock) {
        return ProductJpaEntity.createProduct(name, new BigDecimal(price), stock, List.of(new CategoryJpaEntity()));
    }
}