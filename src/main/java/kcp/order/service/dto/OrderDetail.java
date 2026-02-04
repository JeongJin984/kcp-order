package kcp.order.service.dto;

import kcp.order.service.entity.OrderJpaEntity;
import kcp.order.service.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetail (
    Long id,
    OrderStatus orderStatus,
    LocalDateTime orderDate,
    List<OrderItem> items
) {
    public static OrderDetail from(OrderJpaEntity order){
        return new OrderDetail(
            order.getId(),
            order.getStatus(),
            order.getOrderDate(),
            order.getOrderItems().stream()
                .map(v -> new OrderItem(
                    v.getProduct().getId(), v.getProduct().getName(), v.getCount(), v.getOrderPrice()
                )).toList()
        );
    }

    public record OrderItem(Long productId, String productName, int count, BigDecimal orderPrice){}
}
