package kcp.order.domain.order.service;

import kcp.order.common.exception.BusinessException;
import kcp.order.common.exception.ErrorCode;
import kcp.order.domain.order.dto.OrderCreateCmd;
import kcp.order.domain.order.dto.OrderDetail;
import kcp.order.domain.order.entity.OrderJpaEntity;
import kcp.order.domain.order.entity.OrderStatus;
import kcp.order.domain.order.repository.OrderRepository;
import kcp.order.domain.product.entity.CategoryJpaEntity;
import kcp.order.domain.product.entity.ProductJpaEntity;
import kcp.order.domain.product.repository.ProductRepository;
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
    @DisplayName("주문 상태 변경 실패 - 잘못된 전이")
    void changeStatus_invalidTransition_throwsException() {
        // given
        Long orderId = 1L;
        OrderJpaEntity order = OrderJpaEntity.createOrder(List.of());
        given(orderRepository.findByIdWithProductAndLock(orderId)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.changeStatus(orderId, OrderStatus.COMPLETED))
                .isInstanceOf(BusinessException.class); // InvalidOrderStatusException extends BusinessException (likely)
    }

    private ProductJpaEntity createProduct(String name, int price, int stock) {
        return ProductJpaEntity.createProduct(name, new BigDecimal(price), stock, List.of(new CategoryJpaEntity()));
    }
}