package kcp.order.service;

import kcp.common.exception.BusinessException;
import kcp.common.exception.ErrorCode;
import kcp.common.exception.InvalidOrderStatusException;
import kcp.order.service.dto.OrderCreateCmd;
import kcp.order.service.dto.OrderDetail;
import kcp.order.service.dto.OrderSearchCmd;
import kcp.order.service.entity.OrderItemJpaEntity;
import kcp.order.service.entity.OrderJpaEntity;
import kcp.order.service.entity.OrderStatus;
import kcp.order.service.repository.OrderRepository;
import kcp.product.service.entity.ProductJpaEntity;
import kcp.product.service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * 주문 조회(다건, 페이징)
     */
    public Page<OrderDetail> getOrders(OrderSearchCmd cmd, Pageable page) {
        return orderRepository.findSliceOrderByOrderDate(cmd, page);
    }

    /**
     * 주문 조회 (단건)
     */
    public OrderDetail getOrder(Long orderId) {
        OrderJpaEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "order not found: orderId = {}", orderId));
        return OrderDetail.from(order);
    }

    /**
     * 주문 생성 (기본 상태: WAIT)
     */
    @Transactional
    public OrderDetail registerOrder(OrderCreateCmd request) {
        List<OrderItemJpaEntity> orderItems = request.items().stream()
            .map(itemReq -> {
                // 비관적 락을 적용하여 상품 조회 (동시성 확보)
                ProductJpaEntity product = productRepository.findByIdWithLock(itemReq.productId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "product not found: productId = {}", itemReq.productId()));

                // 주문 상품 엔티티 생성 (이 시점엔 재고 차감 X, 생성만 함)
                return OrderItemJpaEntity.createOrderItem(product, product.getPrice(), itemReq.count());
            })
            .toList();

        OrderJpaEntity order = orderRepository.save(OrderJpaEntity.createOrder(orderItems));
        return OrderDetail.from(order);
    }

    /**
     * 주문 상태 변경 (비즈니스 로직 핵심)
     */
    @Transactional
    public OrderDetail changeStatus(Long orderId, OrderStatus nextStatus) {
        OrderJpaEntity order = orderRepository.findByIdWithProductAndLock(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "order not found: orderId = {}", orderId));

        order.updateOrderStatus(nextStatus);

        return OrderDetail.from(order);
    }
}