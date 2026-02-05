package kcp.order.service;

import kcp.common.exception.BusinessException;
import kcp.common.exception.ErrorCode;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        // 1. 요청 내 상품 ID별로 수량 합산 (중복 제거 효과)
        // Map<Long, Integer> : productId -> totalCount
        Map<Long, Integer> productCountMap = request.items().stream()
            .collect(Collectors.groupingBy(
                OrderCreateCmd.OrderItem::productId,
                Collectors.summingInt(OrderCreateCmd.OrderItem::count)
            ));

        List<Long> requestedIds = new ArrayList<>(productCountMap.keySet());

        // 2. IN 절 일괄 조회 (중복 없는 ID 리스트로 조회)
        List<ProductJpaEntity> products = productRepository.findAllByIds(requestedIds);

        // 3. 존재하지 않는 상품이 있는지 검증
        if (products.size() != requestedIds.size()) {
            List<Long> foundIds = products.stream()
                .map(ProductJpaEntity::getId)
                .toList();

            // 요청한 ID 중 DB에 없는 것들만 필터링
            List<Long> missingIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "존재하지 않는 상품 ID가 포함되어 있습니다: " + missingIds);
        }

        // 4. 주문 상세 엔티티 생성
        List<OrderItemJpaEntity> orderItems = products.stream()
            .map(product -> {
                // 합산된 수량 가져오기
                int totalCount = productCountMap.get(product.getId());
                return OrderItemJpaEntity.createOrderItem(product, product.getPrice(), totalCount);
            })
            .toList();

        // 5. 주문 저장
        OrderJpaEntity order = orderRepository.save(OrderJpaEntity.createOrder(orderItems));

        return OrderDetail.from(order);
    }

    /**
     * 주문 상태 변경 (비즈니스 로직 핵심)
     */
    @Transactional
    public OrderDetail changeStatus(Long orderId, OrderStatus nextStatus) {
        OrderJpaEntity order = orderRepository.findByIdAndLock(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "order not found: orderId = {}", orderId));

        // [핵심] 재고 변경이 가능한 상태 변경(COMPLETED/CANCELED 등)이라면
        // Product row를 명시적으로 FOR UPDATE로 잠근 뒤에 재고를 깎아야 정합성이 보장됩니다.
        List<Long> productIdsToLock = order.getOrderItems().stream()
            .map(OrderItemJpaEntity::getProduct)
            .map(ProductJpaEntity::getId)
            .distinct()
            .sorted() // 데드락 확률 감소: 항상 동일한 순서로 락 획득
            .toList();

        if (!productIdsToLock.isEmpty()) {
            productRepository.findByIdsWithLock(productIdsToLock);
        }

        order.updateOrderStatus(nextStatus);

        return OrderDetail.from(order);
    }
}