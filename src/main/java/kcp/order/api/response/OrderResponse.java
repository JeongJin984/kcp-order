package kcp.order.api.response;

import kcp.order.domain.order.dto.OrderDetail;
import kcp.order.domain.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {
    private Long orderId;
    private OrderStatus status;
    private LocalDateTime orderDate;
    private List<OrderItem> orderItems;

    @Getter
    @Builder
    public static class OrderItem {
        private String productName;
        private BigDecimal orderPrice;
        private int count;
    }

    public static OrderResponse from(OrderDetail order) {
        return OrderResponse.builder()
                .orderId(order.id())
                .status(order.orderStatus())
                .orderDate(order.orderDate())
                .orderItems(order.items().stream()
                        .map(item -> new OrderItem(
                            item.productName(),
                            item.orderPrice(),
                            item.count()
                        ))
                        .toList())
                .build();
    }
}