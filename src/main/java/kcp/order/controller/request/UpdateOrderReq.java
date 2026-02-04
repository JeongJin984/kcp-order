package kcp.order.controller.request;

import kcp.order.service.entity.OrderStatus;

public record UpdateOrderReq (
    OrderStatus orderStatus
) {
}
