package kcp.order.api.request;

import kcp.order.domain.order.entity.OrderStatus;

public record UpdateOrderReq (
    OrderStatus orderStatus
) {
}
